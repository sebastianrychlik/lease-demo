package com.leasedemo.lease.application.scoring;

import com.leasedemo.lease.application.model.ApplicationStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Deterministic DEMO credit-scoring simulation (M5.3 §12–§14).
 *
 * <p>Pure: no repository, no HTTP, no Spring Security, no randomness. Given
 * the same input it always produces the same score/decision.
 *
 * <p>This is explicitly NOT a real credit-bureau/BIK integration — see the
 * M5.3 scope guardrails.
 */
@Service
public class CreditScoringService {

    private static final BigDecimal BASE_SCORE = new BigDecimal("850");
    private static final BigDecimal AFFORDABILITY_WEIGHT = new BigDecimal("200");

    private static final int MIN_SCORE = 300;
    private static final int MAX_SCORE = 850;

    /** Single obvious place for the decision thresholds (M5.3 §14). */
    private static final int APPROVED_THRESHOLD = 700;
    private static final int REVIEW_THRESHOLD = 600;

    private static final MathContext MATH_CONTEXT = new MathContext(20, RoundingMode.HALF_UP);

    /**
     * @param monthlyNetIncome        applicant's monthly net income, must be {@code > 0}
     * @param monthlyObligations      applicant's existing monthly obligations, {@code >= 0}
     * @param leaseMonthlyPayment     authoritative backend-calculated lease monthly payment
     * @param insuranceMonthlyPremium authoritative backend-calculated insurance premium
     */
    public Result score(
            BigDecimal monthlyNetIncome,
            BigDecimal monthlyObligations,
            BigDecimal leaseMonthlyPayment,
            BigDecimal insuranceMonthlyPremium) {

        BigDecimal monthlyBurden = monthlyObligations
                .add(leaseMonthlyPayment)
                .add(insuranceMonthlyPremium);

        BigDecimal affordabilityRatio = monthlyBurden.divide(monthlyNetIncome, MATH_CONTEXT);

        BigDecimal rawScore = BASE_SCORE.subtract(affordabilityRatio.multiply(AFFORDABILITY_WEIGHT, MATH_CONTEXT));

        int score = clamp(rawScore.setScale(0, RoundingMode.HALF_UP).intValueExact());

        return new Result(score, decisionFor(score));
    }

    private static int clamp(int score) {
        return Math.max(MIN_SCORE, Math.min(MAX_SCORE, score));
    }

    private static ApplicationStatus decisionFor(int score) {
        if (score >= APPROVED_THRESHOLD) {
            return ApplicationStatus.APPROVED;
        }
        if (score >= REVIEW_THRESHOLD) {
            return ApplicationStatus.REVIEW;
        }
        return ApplicationStatus.REJECTED;
    }

    /** Deterministic scoring result: clamped 300–850 score + decision. */
    public record Result(int score, ApplicationStatus status) {
    }
}
