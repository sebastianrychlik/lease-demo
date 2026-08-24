package com.leasedemo.service;

import com.leasedemo.dto.CustomerCreateRequest;
import com.leasedemo.dto.CustomerListItemResponse;
import com.leasedemo.dto.CustomerProfileResponse;
import com.leasedemo.dto.CustomerResponse;
import com.leasedemo.dto.PageResponse;
import com.leasedemo.entity.Customer;
import com.leasedemo.entity.Gender;
import com.leasedemo.exception.CustomerProfileNotFoundException;
import com.leasedemo.exception.DuplicateCustomerException;
import com.leasedemo.exception.InvalidPeselException;
import com.leasedemo.exception.InvalidSortFieldException;
import com.leasedemo.mapper.CustomerMapper;
import com.leasedemo.repository.CustomerRepository;
import com.leasedemo.security.crypto.AesGcmEncryptionService;
import com.leasedemo.security.crypto.HmacLookupHashService;
import com.leasedemo.util.PeselValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerService")
class CustomerServiceTest {

    private static final String PESEL = "44051401359";

    @Mock
    private PeselValidator peselValidator;

    @Mock
    private AesGcmEncryptionService encryptionService;

    @Mock
    private HmacLookupHashService lookupHashService;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerMapper customerMapper;

    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerService(
                peselValidator, encryptionService, lookupHashService, customerRepository, customerMapper);
    }

    private static final String KEYCLOAK_USER_ID = "keycloak-sub-123";

    private CustomerCreateRequest request() {
        return new CustomerCreateRequest(
                "Jan", "Kowalski", "jan.kowalski@example.com", "+48123456789",
                LocalDate.of(1944, 5, 14), Gender.MALE, PESEL);
    }

    @Test
    @DisplayName("validates, encrypts, hashes and persists a new customer")
    void createCustomer_validRequest_persistsEncryptedCustomer() {
        CustomerCreateRequest request = request();

        when(customerRepository.existsByKeycloakUserId(KEYCLOAK_USER_ID)).thenReturn(false);
        when(lookupHashService.hash(PESEL)).thenReturn("lookup-hash");
        when(customerRepository.existsByPeselLookup("lookup-hash")).thenReturn(false);
        when(encryptionService.encrypt(PESEL)).thenReturn("cipher-text");

        Customer saved = new Customer(
                KEYCLOAK_USER_ID, request.firstName(), request.lastName(), request.email(),
                request.phoneNumber(), request.dateOfBirth(), request.gender(),
                "cipher-text", "lookup-hash");
        when(customerRepository.save(any(Customer.class))).thenReturn(saved);

        CustomerResponse expectedResponse = new CustomerResponse(
                UUID.randomUUID(), KEYCLOAK_USER_ID, request.firstName(), request.lastName(),
                request.email(), request.phoneNumber(), request.dateOfBirth(), request.gender(),
                saved.getCreatedAt(), saved.getUpdatedAt());
        when(customerMapper.toResponse(saved)).thenReturn(expectedResponse);

        CustomerResponse response = customerService.createCustomer(KEYCLOAK_USER_ID, request);

        verify(peselValidator).validate(PESEL, request.dateOfBirth(), request.gender());

        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(customerCaptor.capture());
        Customer persisted = customerCaptor.getValue();
        assertThat(persisted.getPeselEncrypted()).isEqualTo("cipher-text");
        assertThat(persisted.getPeselLookup()).isEqualTo("lookup-hash");
        assertThat(persisted.getKeycloakUserId()).isEqualTo(KEYCLOAK_USER_ID);

        assertThat(response).isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("rejects a PESEL that fails validation before touching the repository")
    void createCustomer_invalidPesel_propagatesAndSkipsPersistence() {
        CustomerCreateRequest request = request();
        when(customerRepository.existsByKeycloakUserId(KEYCLOAK_USER_ID)).thenReturn(false);
        org.mockito.Mockito.doThrow(new InvalidPeselException("bad pesel"))
                .when(peselValidator).validate(anyString(), any(), any());

        assertThatThrownBy(() -> customerService.createCustomer(KEYCLOAK_USER_ID, request))
                .isInstanceOf(InvalidPeselException.class);

        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejects a duplicate PESEL without encrypting or persisting")
    void createCustomer_duplicatePesel_throwsAndSkipsPersistence() {
        CustomerCreateRequest request = request();
        when(customerRepository.existsByKeycloakUserId(KEYCLOAK_USER_ID)).thenReturn(false);
        when(lookupHashService.hash(PESEL)).thenReturn("lookup-hash");
        when(customerRepository.existsByPeselLookup("lookup-hash")).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(KEYCLOAK_USER_ID, request))
                .isInstanceOf(DuplicateCustomerException.class);

        verify(encryptionService, never()).encrypt(anyString());
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejects a duplicate authenticated Keycloak identity before PESEL validation")
    void createCustomer_duplicateKeycloakIdentity_throwsAndSkipsPeselValidation() {
        CustomerCreateRequest request = request();
        when(customerRepository.existsByKeycloakUserId(KEYCLOAK_USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(KEYCLOAK_USER_ID, request))
                .isInstanceOf(DuplicateCustomerException.class);

        verify(peselValidator, never()).validate(anyString(), any(), any());
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("fails safely when the authenticated subject is blank")
    void createCustomer_blankKeycloakUserId_throwsIllegalStateException() {
        CustomerCreateRequest request = request();

        assertThatThrownBy(() -> customerService.createCustomer("  ", request))
                .isInstanceOf(IllegalStateException.class);

        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("security boundary: persisted keycloakUserId comes from the trusted "
            + "authenticated subject, never from request data")
    void createCustomer_bindsIdentityFromAuthenticatedSubjectNotRequestBody() {
        String trustedSubject = "trusted-user-123";
        CustomerCreateRequest request = request();

        when(customerRepository.existsByKeycloakUserId(trustedSubject)).thenReturn(false);
        when(lookupHashService.hash(PESEL)).thenReturn("lookup-hash");
        when(customerRepository.existsByPeselLookup("lookup-hash")).thenReturn(false);
        when(encryptionService.encrypt(PESEL)).thenReturn("cipher-text");
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(customerMapper.toResponse(any(Customer.class))).thenReturn(
                new CustomerResponse(UUID.randomUUID(), trustedSubject, request.firstName(), request.lastName(),
                        request.email(), request.phoneNumber(), request.dateOfBirth(), request.gender(),
                        null, null));

        customerService.createCustomer(trustedSubject, request);

        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(customerCaptor.capture());
        assertThat(customerCaptor.getValue().getKeycloakUserId()).isEqualTo(trustedSubject);
    }

    // ── M4.5: self-service GET /api/customers/me ───────────────────────────────

    @Test
    @DisplayName("getCurrentCustomerProfile: resolves profile by keycloakUserId (JWT.sub)")
    void getCurrentCustomerProfile_existingCustomer_returnsProfile() {
        Customer customer = sampleCustomer();
        CustomerProfileResponse profile = new CustomerProfileResponse(
                customer.getId(), "Jan", "Kowalski", "jan.kowalski@example.com",
                "+48123456789", LocalDate.of(1944, 5, 14), Gender.MALE, Instant.now());

        when(customerRepository.findByKeycloakUserId(KEYCLOAK_USER_ID)).thenReturn(Optional.of(customer));
        when(customerMapper.toProfileResponse(customer)).thenReturn(profile);

        CustomerProfileResponse result = customerService.getCurrentCustomerProfile(KEYCLOAK_USER_ID);

        assertThat(result).isEqualTo(profile);
    }

    @Test
    @DisplayName("getCurrentCustomerProfile: no matching customer throws CustomerProfileNotFoundException")
    void getCurrentCustomerProfile_noCustomer_throws() {
        when(customerRepository.findByKeycloakUserId(KEYCLOAK_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCurrentCustomerProfile(KEYCLOAK_USER_ID))
                .isInstanceOf(CustomerProfileNotFoundException.class);
    }

    // ── M4.4: Admin Customer list query ──────────────────────────────────────

    private Customer sampleCustomer() {
        return new Customer(
                KEYCLOAK_USER_ID, "Jan", "Kowalski", "jan.kowalski@example.com",
                "+48123456789", LocalDate.of(1944, 5, 14), Gender.MALE,
                "cipher-text", "lookup-hash");
    }

    @Test
    @DisplayName("getCustomers: default page/size uses page=0,size=20 and default sort")
    void getCustomers_defaults_usesPageZeroSizeTwentyAndDefaultSort() {
        Page<Customer> page = new PageImpl<>(List.of(sampleCustomer()));
        when(customerRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);
        when(customerMapper.toListItemResponse(any(Customer.class))).thenReturn(
                new CustomerListItemResponse(UUID.randomUUID(), "Jan", "Kowalski",
                        "jan.kowalski@example.com", "+48123456789",
                        LocalDate.of(1944, 5, 14), Gender.MALE, Instant.now()));

        customerService.getCustomers(0, 20, null, null, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(customerRepository).findAll(any(Specification.class), pageableCaptor.capture());
        Pageable used = pageableCaptor.getValue();
        assertThat(used.getPageNumber()).isEqualTo(0);
        assertThat(used.getPageSize()).isEqualTo(20);
        assertThat(used.getSort()).isEqualTo(
                Sort.by(Sort.Order.asc("lastName"), Sort.Order.asc("firstName")));
    }

    @Test
    @DisplayName("getCustomers: page size is clamped to the maximum of 100")
    void getCustomers_oversizedPageSize_isClampedToMax() {
        when(customerRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        customerService.getCustomers(0, 10_000, null, null, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(customerRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("getCustomers: negative page is clamped to zero")
    void getCustomers_negativePage_isClampedToZero() {
        when(customerRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        customerService.getCustomers(-5, 20, null, null, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(customerRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
    }

    @Test
    @DisplayName("getCustomers: supported sort field with descending direction is honored")
    void getCustomers_supportedSortFieldDescending_isHonored() {
        when(customerRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        customerService.getCustomers(0, 20, null, "email", "desc");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(customerRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort())
                .isEqualTo(Sort.by(Sort.Direction.DESC, "email"));
    }

    @Test
    @DisplayName("getCustomers: unsupported sort field throws InvalidSortFieldException")
    void getCustomers_unsupportedSortField_throws() {
        assertThatThrownBy(() -> customerService.getCustomers(0, 20, null, "peselLookup", "asc"))
                .isInstanceOf(InvalidSortFieldException.class);

        verify(customerRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("getCustomers: maps Page<Customer> content into PageResponse<CustomerListItemResponse>")
    void getCustomers_mapsPageContentAndMetadata() {
        Customer customer = sampleCustomer();
        Page<Customer> page = new PageImpl<>(List.of(customer), PageRequest.of(0, 20), 1);
        when(customerRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        CustomerListItemResponse mapped = new CustomerListItemResponse(
                UUID.randomUUID(), "Jan", "Kowalski", "jan.kowalski@example.com",
                "+48123456789", LocalDate.of(1944, 5, 14), Gender.MALE, Instant.now());
        when(customerMapper.toListItemResponse(customer)).thenReturn(mapped);

        PageResponse<CustomerListItemResponse> result =
                customerService.getCustomers(0, 20, null, null, null);

        assertThat(result.content()).containsExactly(mapped);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.first()).isTrue();
        assertThat(result.last()).isTrue();
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(20);
    }
}
