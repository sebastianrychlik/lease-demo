package com.leasedemo.service;

import com.leasedemo.dto.CustomerCreateRequest;
import com.leasedemo.dto.CustomerResponse;
import com.leasedemo.entity.Customer;
import com.leasedemo.entity.Gender;
import com.leasedemo.exception.DuplicateCustomerException;
import com.leasedemo.exception.InvalidPeselException;
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

import java.time.LocalDate;
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

    private CustomerCreateRequest request() {
        return new CustomerCreateRequest(
                "keycloak-sub-123", "Jan", "Kowalski", "jan.kowalski@example.com", "+48123456789",
                LocalDate.of(1944, 5, 14), Gender.MALE, PESEL);
    }

    @Test
    @DisplayName("validates, encrypts, hashes and persists a new customer")
    void createCustomer_validRequest_persistsEncryptedCustomer() {
        CustomerCreateRequest request = request();

        when(lookupHashService.hash(PESEL)).thenReturn("lookup-hash");
        when(customerRepository.existsByPeselLookup("lookup-hash")).thenReturn(false);
        when(encryptionService.encrypt(PESEL)).thenReturn("cipher-text");

        Customer saved = new Customer(
                request.keycloakUserId(), request.firstName(), request.lastName(), request.email(),
                request.phoneNumber(), request.dateOfBirth(), request.gender(),
                "cipher-text", "lookup-hash");
        when(customerRepository.save(any(Customer.class))).thenReturn(saved);

        CustomerResponse expectedResponse = new CustomerResponse(
                UUID.randomUUID(), request.keycloakUserId(), request.firstName(), request.lastName(),
                request.email(), request.phoneNumber(), request.dateOfBirth(), request.gender(),
                saved.getCreatedAt(), saved.getUpdatedAt());
        when(customerMapper.toResponse(saved)).thenReturn(expectedResponse);

        CustomerResponse response = customerService.createCustomer(request);

        verify(peselValidator).validate(PESEL, request.dateOfBirth(), request.gender());

        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(customerCaptor.capture());
        Customer persisted = customerCaptor.getValue();
        assertThat(persisted.getPeselEncrypted()).isEqualTo("cipher-text");
        assertThat(persisted.getPeselLookup()).isEqualTo("lookup-hash");

        assertThat(response).isEqualTo(expectedResponse);
    }

    @Test
    @DisplayName("rejects a PESEL that fails validation before touching the repository")
    void createCustomer_invalidPesel_propagatesAndSkipsPersistence() {
        CustomerCreateRequest request = request();
        org.mockito.Mockito.doThrow(new InvalidPeselException("bad pesel"))
                .when(peselValidator).validate(anyString(), any(), any());

        assertThatThrownBy(() -> customerService.createCustomer(request))
                .isInstanceOf(InvalidPeselException.class);

        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejects a duplicate PESEL without encrypting or persisting")
    void createCustomer_duplicatePesel_throwsAndSkipsPersistence() {
        CustomerCreateRequest request = request();
        when(lookupHashService.hash(PESEL)).thenReturn("lookup-hash");
        when(customerRepository.existsByPeselLookup("lookup-hash")).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(request))
                .isInstanceOf(DuplicateCustomerException.class);

        verify(encryptionService, never()).encrypt(anyString());
        verify(customerRepository, never()).save(any());
    }
}
