package com.leasedemo.service;

import com.leasedemo.dto.CustomerCreateRequest;
import com.leasedemo.dto.CustomerResponse;
import com.leasedemo.entity.Customer;
import com.leasedemo.exception.DuplicateCustomerException;
import com.leasedemo.mapper.CustomerMapper;
import com.leasedemo.repository.CustomerRepository;
import com.leasedemo.security.crypto.AesGcmEncryptionService;
import com.leasedemo.security.crypto.HmacLookupHashService;
import com.leasedemo.util.PeselValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
}
