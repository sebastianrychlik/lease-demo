package com.leasedemo.lease.application.pdf;

import com.leasedemo.entity.Customer;
import com.leasedemo.entity.Gender;
import com.leasedemo.lease.application.dto.InsuranceCoverageSnapshot;
import com.leasedemo.lease.application.entity.LeaseApplication;
import com.leasedemo.lease.application.insurance.InsuranceCoverageCode;
import com.leasedemo.lease.application.model.ApplicationStatus;
import com.leasedemo.lease.quote.model.LeaseCurrency;
import com.leasedemo.lease.quote.model.LeaseType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LeaseApplicationPdfService")
class LeaseApplicationPdfServiceTest {

    private final LeaseApplicationPdfService service = new LeaseApplicationPdfService();

    @Test
    @DisplayName("generates a non-empty, valid PDF from a demo application snapshot (M5.5)")
    void generatesValidPdf() {
        Customer customer = new Customer(
                "keycloak-sub-123", "Jane", "Doe", "jane@example.com", null,
                LocalDate.of(1990, 1, 1), Gender.FEMALE, "encrypted", "lookup-hash");

        LeaseApplication application = new LeaseApplication(
                customer, "STANDARD_CAR_PL", "Standard Car Leasing", ApplicationStatus.APPROVED, 750,
                new BigDecimal("15000"), new BigDecimal("1000"),
                new BigDecimal("45000"), LeaseCurrency.EUR, LeaseCurrency.PLN,
                new BigDecimal("4.30"), LocalDate.now(), new BigDecimal("193500.00"),
                36, new BigDecimal("20"), new BigDecimal("38700.00"),
                new BigDecimal("15"), new BigDecimal("29025.00"),
                LeaseType.OPERATING, new BigDecimal("7.20"),
                new BigDecimal("154800.00"), new BigDecimal("3728.38"),
                new BigDecimal("193500.00"), new BigDecimal("500.00"),
                new BigDecimal("39.00"),
                List.of(new InsuranceCoverageSnapshot(InsuranceCoverageCode.GAP, "STANDARD", new BigDecimal("79"))));

        byte[] pdf = service.generate(application);

        assertThat(pdf).isNotNull();
        assertThat(pdf.length).isGreaterThan(500);
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }
}
