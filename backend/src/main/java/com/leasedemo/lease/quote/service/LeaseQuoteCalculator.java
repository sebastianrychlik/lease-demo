package com.leasedemo.lease.quote.service;

import com.leasedemo.lease.quote.model.LeaseType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Deterministic demo lease-calculation engine.
 *
 * <p>This is NOT a reproduction of any real bank's proprietary leasing
 * model — it is a simple, understandable amortization model with a final
 * balloon/residual (buyout) payment, suitable for an interview/demo
 * application. All monetary amounts are {@link BigDecimal}, rounded to 2
 * decimal places (PLN convention).
 *
 * <p>This class contains only the arithmetic — it has no knowledge of NBP,
 * HTTP, or DTOs, so it stays easy to unit test and easy to move to
 * configuration later (M5.2+).
 */
@Component
public class LeaseQuoteCalculator {

    /** Demo nominal annual interest rates per lease type. Move to configuration when needed. */
    private static final Map<LeaseType, BigDecimal> ANNUAL_RATE_PERCENT = Map.of(
            LeaseType.OPERATING, new BigDecimal("7.20"),
            LeaseType.FINANCIAL, new BigDecimal("6.90")
    );

    private static final BigDecimal VAT_DIVISOR = new BigDecimal("1.23");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final int MONEY_SCALE = 2;
    private static final MathContext MATH_CONTEXT = new MathContext(20, RoundingMode.HALF_UP);

    public BigDecimal annualRatePercent(LeaseType leaseType) {
        return ANNUAL_RATE_PERCENT.get(leaseType);
    }

    /** Result of the demo lease calculation, all amounts already rounded to 2 decimal places. */
    public record Result(
            BigDecimal vehiclePricePln,
            BigDecimal initialPaymentPln,
            BigDecimal buyoutPln,
            BigDecimal financedAmountPln,
            BigDecimal monthlyPaymentPln,
            BigDecimal totalLeaseCostPln,
            BigDecimal estimatedVatPln
    ) {
    }

    /**
     * Runs the demo lease calculation described in the M5.1 specification.
     *
     * @param vehiclePricePln        vehicle price already converted to PLN
     * @param initialPaymentPercent  0..45
     * @param buyoutPercent          1..40
     * @param termMonths             one of 24, 36, 48, 60
     * @param leaseType              OPERATING or FINANCIAL
     */
    public Result calculate(
            BigDecimal vehiclePricePln,
            BigDecimal initialPaymentPercent,
            BigDecimal buyoutPercent,
            int termMonths,
            LeaseType leaseType
    ) {
        BigDecimal annualRatePercent = annualRatePercent(leaseType);

        BigDecimal initialPaymentPln = vehiclePricePln
                .multiply(initialPaymentPercent, MATH_CONTEXT)
                .divide(ONE_HUNDRED, MATH_CONTEXT);

        BigDecimal buyoutPln = vehiclePricePln
                .multiply(buyoutPercent, MATH_CONTEXT)
                .divide(ONE_HUNDRED, MATH_CONTEXT);

        BigDecimal amountAfterInitialPayment = vehiclePricePln.subtract(initialPaymentPln);

        BigDecimal monthlyRate = annualRatePercent
                .divide(new BigDecimal("12"), MATH_CONTEXT)
                .divide(ONE_HUNDRED, MATH_CONTEXT);

        BigDecimal onePlusMonthlyRate = BigDecimal.ONE.add(monthlyRate, MATH_CONTEXT);
        BigDecimal growthFactor = onePlusMonthlyRate.pow(termMonths, MATH_CONTEXT);

        BigDecimal presentValueOfBuyout = buyoutPln.divide(growthFactor, MATH_CONTEXT);

        BigDecimal amortizedAmount = amountAfterInitialPayment.subtract(presentValueOfBuyout);
        if (amortizedAmount.signum() < 0) {
            // Guard against pathological input combinations (e.g. buyout so high the
            // present value already exceeds the financed amount) producing a negative
            // amortized base, which would otherwise flip the sign of monthlyPayment.
            amortizedAmount = BigDecimal.ZERO;
        }

        BigDecimal negativeExponentFactor = BigDecimal.ONE.divide(growthFactor, MATH_CONTEXT);
        BigDecimal denominator = BigDecimal.ONE.subtract(negativeExponentFactor, MATH_CONTEXT);

        BigDecimal monthlyPayment;
        if (denominator.compareTo(BigDecimal.ZERO) <= 0) {
            // Defensive fallback — should not occur for a positive monthlyRate/termMonths,
            // but protects the calculation from division by zero / negative results.
            monthlyPayment = BigDecimal.ZERO;
        } else {
            monthlyPayment = amortizedAmount
                    .multiply(monthlyRate, MATH_CONTEXT)
                    .divide(denominator, MATH_CONTEXT);
        }
        if (monthlyPayment.signum() < 0) {
            monthlyPayment = BigDecimal.ZERO;
        }

        BigDecimal totalLeaseCost = initialPaymentPln
                .add(monthlyPayment.multiply(BigDecimal.valueOf(termMonths), MATH_CONTEXT))
                .add(buyoutPln);

        BigDecimal estimatedVat = totalLeaseCost.subtract(totalLeaseCost.divide(VAT_DIVISOR, MATH_CONTEXT));

        return new Result(
                round(vehiclePricePln),
                round(initialPaymentPln),
                round(buyoutPln),
                round(amountAfterInitialPayment),
                round(monthlyPayment),
                round(totalLeaseCost),
                round(estimatedVat)
        );
    }

    private static BigDecimal round(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
