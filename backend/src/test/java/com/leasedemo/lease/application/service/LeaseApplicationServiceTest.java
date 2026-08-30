package com.leasedemo.lease.application.service;

import com.leasedemo.entity.Customer;
import com.leasedemo.entity.Gender;
import com.leasedemo.exception.CustomerProfileNotFoundException;
import com.leasedemo.lease.application.dto.CreateLeaseApplicationRequest;
import com.leasedemo.lease.application.dto.LeaseApplicationResponse;
import com.leasedemo.lease.application.entity.LeaseApplication;
import com.leasedemo.lease.application.exception.InvalidApplicationInputException;
import com.leasedemo.lease.application.insurance.InsurancePremiumCalculator;
import com.leasedemo.lease.application.model.ApplicationStatus;
import com.leasedemo.lease.application.scoring.CreditScoringService;
import com.leasedemo.lease.quote.dto.LeaseQuoteRequest;
import com.leasedemo.lease.quote.dto.LeaseQuoteResponse;
import com.leasedemo.lease.quote.model.LeaseCurrency;
import com.leasedemo.lease.quote.model.LeaseType;
import com.leasedemo.lease.quote.service.LeaseQuoteService;
import com.leasedemo.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaseApplicationService")
class LeaseApplicationServiceTest {

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private LeaseQuoteService leaseQuoteService;
    @Mock
    private LeaseApplicationPersistenceService persistenceService;

    private LeaseApplicationService service;

    private Customer customer;

    @BeforeEach
    void setUp() {
        service = new LeaseApplicationService(
                customerRepository, leaseQuoteService, new InsurancePremiumCalculator(),
                new CreditScoringService(), persistenceService);

        customer = new Customer(
                "keycloak-sub-123", "Jane", "Doe", "jane@example.com", null,
                LocalDate.of(1990, 1, 1), Gender.FEMALE, "encrypted", "lookup-hash");
    }

    private CreateLeaseApplicationRequest defaultRequest() {
        return new CreateLeaseApplicationRequest(
                "STANDARD_CAR_PL", new BigDecimal("45000"), LeaseCurrency.EUR, 36,
                new BigDecimal("20"), new BigDecimal("15"), LeaseType.OPERATING,
                List.of(), new BigDecimal("15000"), new BigDecimal("1000"));
    }

    private LeaseQuoteResponse defaultQuoteResponse() {
        return new LeaseQuoteResponse(
                "STANDARD_CAR_PL", "Standard Car Leasing",
                new BigDecimal("45000"), LeaseCurrency.EUR,
                LeaseCurrency.PLN, new BigDecimal("4.30"), "2024-01-01", new BigDecimal("193500.00"),
                36, new BigDecimal("20"), new BigDecimal("38700.00"),
                new BigDecimal("15"), new BigDecimal("29025.00"),
                LeaseType.OPERATING, new BigDecimal("7.20"),
                new BigDecimal("154800.00"), new BigDecimal("3728.38"),
                new BigDecimal("173000.00"), new BigDecimal("30000.00"));
    }

    @Test
    @DisplayName("resolves Customer exclusively from JWT identity, never from the request body")
    void resolvesCustomerFromJwtIdentity() {
        when(customerRepository.findByKeycloakUserId("keycloak-sub-123")).thenReturn(Optional.of(customer));
        when(leaseQuoteService.calculate(any())).thenReturn(defaultQuoteResponse());
        when(persistenceService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.submit("keycloak-sub-123", defaultRequest());

        verify(customerRepository).findByKeycloakUserId("keycloak-sub-123");
    }

    @Test
    @DisplayName("uses the authoritative LeaseQuoteService result, not any client-supplied value")
    void usesAuthoritativeQuote() {
        when(customerRepository.findByKeycloakUserId(any())).thenReturn(Optional.of(customer));
        LeaseQuoteResponse quote = defaultQuoteResponse();
        when(leaseQuoteService.calculate(any(LeaseQuoteRequest.class))).thenReturn(quote);
        ArgumentCaptor<LeaseApplication> captor = ArgumentCaptor.forClass(LeaseApplication.class);
        when(persistenceService.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        LeaseApplicationResponse response = service.submit("keycloak-sub-123", defaultRequest());

        LeaseApplication persisted = captor.getValue();
        assertThat(persisted.getMonthlyPayment()).isEqualByComparingTo(quote.monthlyPayment());
        assertThat(persisted.getExchangeRate()).isEqualByComparingTo(quote.exchangeRate());
        assertThat(response.monthlyPayment()).isEqualByComparingTo(quote.monthlyPayment());
        assertThat(response.estimatedMonthlyTotal())
                .isEqualByComparingTo(quote.monthlyPayment().add(persisted.getInsuranceMonthlyPremium()));
    }

    @Test
    @DisplayName("credit score/status are computed server-side and never supplied by the frontend")
    void computesScoreAndStatusServerSide() {
        when(customerRepository.findByKeycloakUserId(any())).thenReturn(Optional.of(customer));
        when(leaseQuoteService.calculate(any())).thenReturn(defaultQuoteResponse());
        when(persistenceService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LeaseApplicationResponse response = service.submit("keycloak-sub-123", defaultRequest());

        assertThat(response.creditScore()).isBetween(300, 850);
        assertThat(response.status()).isIn((Object[]) ApplicationStatus.values());
    }

    @Test
    @DisplayName("throws when the authenticated identity has no Customer profile")
    void throwsWhenNoCustomerProfile() {
        when(customerRepository.findByKeycloakUserId(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submit("unknown-sub", defaultRequest()))
                .isInstanceOf(CustomerProfileNotFoundException.class);
    }

    @Test
    @DisplayName("rejects non-positive monthlyNetIncome before persisting")
    void rejectsInvalidFinancialInput() {
        when(customerRepository.findByKeycloakUserId(any())).thenReturn(Optional.of(customer));
        CreateLeaseApplicationRequest invalidRequest = new CreateLeaseApplicationRequest(
                "STANDARD_CAR_PL", new BigDecimal("45000"), LeaseCurrency.EUR, 36,
                new BigDecimal("20"), new BigDecimal("15"), LeaseType.OPERATING,
                List.of(), BigDecimal.ZERO, new BigDecimal("1000"));

        assertThatThrownBy(() -> service.submit("keycloak-sub-123", invalidRequest))
                .isInstanceOf(InvalidApplicationInputException.class);

        verify(persistenceService, never()).save(any());
    }
}
