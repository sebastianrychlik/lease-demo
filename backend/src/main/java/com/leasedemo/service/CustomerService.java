package com.leasedemo.service;

import com.leasedemo.dto.CustomerCreateRequest;
import com.leasedemo.dto.CustomerListItemResponse;
import com.leasedemo.dto.CustomerResponse;
import com.leasedemo.dto.PageResponse;
import com.leasedemo.entity.Customer;
import com.leasedemo.exception.DuplicateCustomerException;
import com.leasedemo.exception.InvalidSortFieldException;
import com.leasedemo.mapper.CustomerMapper;
import com.leasedemo.repository.CustomerRepository;
import com.leasedemo.repository.CustomerSpecifications;
import com.leasedemo.security.crypto.AesGcmEncryptionService;
import com.leasedemo.security.crypto.HmacLookupHashService;
import com.leasedemo.util.PeselValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * Orchestrates customer registration:
 *
 * <ol>
 *   <li>Validate the PESEL (format, checksum, DOB/gender cross-check) via
 *       {@link PeselValidator}.</li>
 *   <li>Derive the deterministic {@code pesel_lookup} hash via
 *       {@link HmacLookupHashService} and reject duplicates.</li>
 *   <li>Encrypt the raw PESEL into {@code pesel_encrypted} via
 *       {@link AesGcmEncryptionService}.</li>
 *   <li>Persist the {@link Customer} entity — the raw PESEL itself is never
 *       stored or logged.</li>
 * </ol>
 *
 * <p><strong>Trust boundary:</strong> {@code keycloakUserId} is trusted,
 * authenticated identity taken from the validated JWT {@code sub} claim
 * (see {@code CustomerController}) — it is never accepted from
 * {@link CustomerCreateRequest}, which is untrusted client-supplied
 * business data.
 */
@Service
public class CustomerService {

    /**
     * Explicit allow-list of client-supplied sort fields for the Admin
     * Customer list query (M4.4). The API owns its supported sort contract
     * instead of passing arbitrary client property names into persistence
     * sorting.
     */
    private static final Set<String> SUPPORTED_SORT_FIELDS =
            Set.of("firstName", "lastName", "email", "dateOfBirth", "createdAt");

    /** Deterministic default sort: last name, then first name, ascending. */
    private static final Sort DEFAULT_SORT = Sort.by(
            Sort.Order.asc("lastName"), Sort.Order.asc("firstName"));

    /** Hard ceiling on requested page size — prevents unbounded row retrieval. */
    private static final int MAX_PAGE_SIZE = 100;

    private final PeselValidator peselValidator;
    private final AesGcmEncryptionService encryptionService;
    private final HmacLookupHashService lookupHashService;
    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    public CustomerService(
            PeselValidator peselValidator,
            AesGcmEncryptionService encryptionService,
            HmacLookupHashService lookupHashService,
            CustomerRepository customerRepository,
            CustomerMapper customerMapper) {
        this.peselValidator = peselValidator;
        this.encryptionService = encryptionService;
        this.lookupHashService = lookupHashService;
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
    }

    /**
     * Creates a new customer.
     *
     * @param keycloakUserId trusted, authenticated identity taken from the
     *                       validated JWT {@code sub} claim — never
     *                       client-controlled
     * @param request        untrusted client-supplied business data
     */
    @Transactional
    public CustomerResponse createCustomer(String keycloakUserId, CustomerCreateRequest request) {
        if (!StringUtils.hasText(keycloakUserId)) {
            // Should never happen for a request that passed JWT authentication —
            // fail safely rather than persist a customer with no owning identity.
            throw new IllegalStateException("Authenticated JWT does not contain a usable subject");
        }

        // Friendly, early failure — the authoritative, concurrency-safe guard is
        // the customers.keycloak_user_id UNIQUE database constraint.
        if (customerRepository.existsByKeycloakUserId(keycloakUserId)) {
            throw new DuplicateCustomerException(
                    "A customer is already registered for this authenticated identity");
        }

        peselValidator.validate(request.pesel(), request.dateOfBirth(), request.gender());

        String peselLookup = lookupHashService.hash(request.pesel());
        if (customerRepository.existsByPeselLookup(peselLookup)) {
            throw new DuplicateCustomerException("A customer with this PESEL is already registered");
        }

        String peselEncrypted = encryptionService.encrypt(request.pesel());

        Customer customer = new Customer(
                keycloakUserId,
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phoneNumber(),
                request.dateOfBirth(),
                request.gender(),
                peselEncrypted,
                peselLookup);

        Customer saved = customerRepository.save(customer);
        return customerMapper.toResponse(saved);
    }

    /**
     * Server-side paged/sorted/searched Admin Customer list query (M4.4).
     *
     * <p>Pagination, sorting, and free-text filtering are all pushed down
     * into a single PostgreSQL query via {@link CustomerRepository}'s
     * {@code JpaSpecificationExecutor} — no in-memory filtering/sorting or
     * loading of unrelated rows occurs.
     *
     * @param page       zero-based page index; negative values are clamped to 0
     * @param size       requested page size; clamped to {@code [1, MAX_PAGE_SIZE]}
     * @param search     optional free-text search over first name / last name / email;
     *                   null/blank behaves as an unfiltered query
     * @param sortField  optional client-requested sort field; must be present in
     *                   {@link #SUPPORTED_SORT_FIELDS} or {@link InvalidSortFieldException} is thrown
     * @param sortDirection optional client-requested sort direction ("asc"/"desc", case-insensitive);
     *                      defaults to ascending
     */
    @Transactional(readOnly = true)
    public PageResponse<CustomerListItemResponse> getCustomers(
            int page, int size, String search, String sortField, String sortDirection) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        Sort sort = resolveSort(sortField, sortDirection);
        Pageable pageable = PageRequest.of(safePage, safeSize, sort);

        Page<Customer> result = customerRepository.findAll(
                CustomerSpecifications.searchByNameOrEmail(search), pageable);

        return PageResponse.from(result, customerMapper::toListItemResponse);
    }

    private Sort resolveSort(String sortField, String sortDirection) {
        if (!StringUtils.hasText(sortField)) {
            return DEFAULT_SORT;
        }
        if (!SUPPORTED_SORT_FIELDS.contains(sortField)) {
            throw new InvalidSortFieldException(
                    "Unsupported sort field: '" + sortField + "'. Supported fields: "
                            + SUPPORTED_SORT_FIELDS);
        }
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direction, sortField);
    }
}
