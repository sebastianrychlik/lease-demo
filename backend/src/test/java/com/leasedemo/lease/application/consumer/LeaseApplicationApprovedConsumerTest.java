package com.leasedemo.lease.application.consumer;

import com.leasedemo.entity.Customer;
import com.leasedemo.entity.Gender;
import com.leasedemo.lease.application.entity.LeaseApplication;
import com.leasedemo.lease.application.event.LeaseApplicationApprovedEvent;
import com.leasedemo.lease.application.mail.LeaseApplicationMailService;
import com.leasedemo.lease.application.model.ApplicationStatus;
import com.leasedemo.lease.application.pdf.LeaseApplicationPdfService;
import com.leasedemo.lease.application.repository.LeaseApplicationRepository;
import com.leasedemo.lease.quote.model.LeaseCurrency;
import com.leasedemo.lease.quote.model.LeaseType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaseApplicationApprovedConsumer")
class LeaseApplicationApprovedConsumerTest {

    @Mock
    private LeaseApplicationRepository leaseApplicationRepository;
    @Mock
    private LeaseApplicationPdfService pdfService;
    @Mock
    private LeaseApplicationMailService mailService;

    private LeaseApplicationApprovedConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new LeaseApplicationApprovedConsumer(leaseApplicationRepository, pdfService, mailService);
    }

    private LeaseApplication demoApplication() {
        Customer customer = new Customer(
                "keycloak-sub-123", "Jane", "Doe", "jane@example.com", null,
                LocalDate.of(1990, 1, 1), Gender.FEMALE, "encrypted", "lookup-hash");

        return new LeaseApplication(
                customer, "STANDARD_CAR_PL", "Standard Car Leasing", ApplicationStatus.APPROVED, 750,
                new BigDecimal("15000"), new BigDecimal("1000"),
                new BigDecimal("45000"), LeaseCurrency.EUR, LeaseCurrency.PLN,
                new BigDecimal("4.30"), LocalDate.now(), new BigDecimal("193500.00"),
                36, new BigDecimal("20"), new BigDecimal("38700.00"),
                new BigDecimal("15"), new BigDecimal("29025.00"),
                LeaseType.OPERATING, new BigDecimal("7.20"),
                new BigDecimal("154800.00"), new BigDecimal("3728.38"),
                new BigDecimal("193500.00"), new BigDecimal("500.00"),
                new BigDecimal("39.00"), List.of());
    }

    @Test
    @DisplayName("loads snapshot, generates PDF, sends email (M5.5)")
    void processesApprovedEventEndToEnd() {
        UUID applicationId = UUID.randomUUID();
        LeaseApplication application = demoApplication();
        LeaseApplicationApprovedEvent event = LeaseApplicationApprovedEvent.of(
                applicationId, ApplicationStatus.APPROVED, "STANDARD_CAR_PL", LeaseCurrency.PLN);

        when(leaseApplicationRepository.findByIdWithCustomer(applicationId)).thenReturn(Optional.of(application));
        byte[] pdfBytes = "%PDF-fake".getBytes();
        when(pdfService.generate(application)).thenReturn(pdfBytes);

        consumer.onApproved(event);

        verify(leaseApplicationRepository).findByIdWithCustomer(applicationId);
        verify(pdfService).generate(application);
        verify(mailService).sendApprovedConfirmation(application, pdfBytes);
    }

    @Test
    @DisplayName("throws when the referenced LeaseApplication cannot be found")
    void throwsWhenApplicationMissing() {
        UUID applicationId = UUID.randomUUID();
        LeaseApplicationApprovedEvent event = LeaseApplicationApprovedEvent.of(
                applicationId, ApplicationStatus.APPROVED, "STANDARD_CAR_PL", LeaseCurrency.PLN);

        when(leaseApplicationRepository.findByIdWithCustomer(applicationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> consumer.onApproved(event))
                .isInstanceOf(LeaseApplicationConsumerException.class);

        verify(pdfService, org.mockito.Mockito.never()).generate(any());
        verify(mailService, org.mockito.Mockito.never()).sendApprovedConfirmation(any(), any());
    }
}
