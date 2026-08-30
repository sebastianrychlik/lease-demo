package com.leasedemo.lease.application.insurance;

import com.leasedemo.lease.application.dto.InsuranceSelectionRequest;
import com.leasedemo.lease.application.exception.InvalidInsuranceConfigurationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("InsurancePremiumCalculator")
class InsurancePremiumCalculatorTest {

    private final InsurancePremiumCalculator calculator = new InsurancePremiumCalculator();

    @Test
    @DisplayName("no selections -> zero premium, empty snapshot")
    void noSelections() {
        InsurancePremiumCalculator.Result result = calculator.calculate(null);

        assertThat(result.coverages()).isEmpty();
        assertThat(result.totalMonthlyPremium()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("GAP PREMIUM = 109")
    void gapPremium() {
        InsurancePremiumCalculator.Result result = calculator.calculate(
                List.of(new InsuranceSelectionRequest(InsuranceCoverageCode.GAP, "PREMIUM")));

        assertThat(result.totalMonthlyPremium()).isEqualByComparingTo(new BigDecimal("109"));
    }

    @Test
    @DisplayName("Assistance EUROPE = 49")
    void assistanceEurope() {
        InsurancePremiumCalculator.Result result = calculator.calculate(
                List.of(new InsuranceSelectionRequest(InsuranceCoverageCode.ASSISTANCE, "EUROPE")));

        assertThat(result.totalMonthlyPremium()).isEqualByComparingTo(new BigDecimal("49"));
    }

    @Test
    @DisplayName("Replacement Car = 39, no option required")
    void replacementCar() {
        InsurancePremiumCalculator.Result result = calculator.calculate(
                List.of(new InsuranceSelectionRequest(InsuranceCoverageCode.REPLACEMENT_CAR, null)));

        assertThat(result.totalMonthlyPremium()).isEqualByComparingTo(new BigDecimal("39"));
    }

    @Test
    @DisplayName("combination sums premiums")
    void combinationSum() {
        InsurancePremiumCalculator.Result result = calculator.calculate(List.of(
                new InsuranceSelectionRequest(InsuranceCoverageCode.GAP, "PREMIUM"),
                new InsuranceSelectionRequest(InsuranceCoverageCode.ASSISTANCE, "EUROPE")));

        assertThat(result.totalMonthlyPremium()).isEqualByComparingTo(new BigDecimal("158"));
        assertThat(result.coverages()).hasSize(2);
    }

    @Test
    @DisplayName("invalid option is rejected")
    void invalidOptionRejected() {
        assertThatThrownBy(() -> calculator.calculate(
                List.of(new InsuranceSelectionRequest(InsuranceCoverageCode.GAP, "EUROPE"))))
                .isInstanceOf(InvalidInsuranceConfigurationException.class);
    }

    @Test
    @DisplayName("duplicate coverage codes are rejected")
    void duplicateCodeRejected() {
        assertThatThrownBy(() -> calculator.calculate(List.of(
                new InsuranceSelectionRequest(InsuranceCoverageCode.GAP, "STANDARD"),
                new InsuranceSelectionRequest(InsuranceCoverageCode.GAP, "PREMIUM"))))
                .isInstanceOf(InvalidInsuranceConfigurationException.class);
    }
}
