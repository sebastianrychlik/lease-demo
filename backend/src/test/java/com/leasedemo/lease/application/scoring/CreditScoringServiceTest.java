package com.leasedemo.lease.application.scoring;

import com.leasedemo.lease.application.model.ApplicationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CreditScoringService")
class CreditScoringServiceTest {

    private final CreditScoringService service = new CreditScoringService();

    @Test
    @DisplayName("low burden relative to income -> APPROVED")
    void lowBurdenApproved() {
        CreditScoringService.Result result = service.score(
                new BigDecimal("15000"), new BigDecimal("1000"), new BigDecimal("3728.38"), new BigDecimal("158"));

        assertThat(result.status()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(result.score()).isBetween(300, 850);
    }

    @Test
    @DisplayName("medium burden relative to income -> REVIEW")
    void mediumBurdenReview() {
        // burden=3100, income=4000 -> ratio=0.775 -> score=850-155=695 (REVIEW: 600<=score<700)
        CreditScoringService.Result result = service.score(
                new BigDecimal("4000"), new BigDecimal("1000"), new BigDecimal("2100"), new BigDecimal("0"));

        assertThat(result.status()).isEqualTo(ApplicationStatus.REVIEW);
    }

    @Test
    @DisplayName("high burden relative to income -> REJECTED")
    void highBurdenRejected() {
        CreditScoringService.Result result = service.score(
                new BigDecimal("3000"), new BigDecimal("2500"), new BigDecimal("2000"), new BigDecimal("200"));

        assertThat(result.status()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(result.score()).isGreaterThanOrEqualTo(300);
    }

    @Test
    @DisplayName("deterministic: identical input always yields identical result")
    void deterministic() {
        CreditScoringService.Result first = service.score(
                new BigDecimal("15000"), new BigDecimal("1000"), new BigDecimal("3728.38"), new BigDecimal("158"));
        CreditScoringService.Result second = service.score(
                new BigDecimal("15000"), new BigDecimal("1000"), new BigDecimal("3728.38"), new BigDecimal("158"));

        assertThat(first.score()).isEqualTo(second.score());
        assertThat(first.status()).isEqualTo(second.status());
    }

    @Test
    @DisplayName("score is clamped between 300 and 850")
    void scoreIsClamped() {
        CreditScoringService.Result veryLowBurden = service.score(
                new BigDecimal("100000"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        CreditScoringService.Result veryHighBurden = service.score(
                new BigDecimal("100"), new BigDecimal("10000"), new BigDecimal("10000"), new BigDecimal("10000"));

        assertThat(veryLowBurden.score()).isLessThanOrEqualTo(850);
        assertThat(veryHighBurden.score()).isGreaterThanOrEqualTo(300);
    }
}
