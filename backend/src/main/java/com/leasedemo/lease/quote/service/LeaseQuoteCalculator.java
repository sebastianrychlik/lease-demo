package com.leasedemo.lease.quote.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

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
 * HTTP, DTOs, JPA, or the current market. The annual rate is no longer a
 * hard-coded per-{@code LeaseType} map here: it is resolved from the
 * selected {@code LeaseProduct} configuration (M5.1.2) and passed in as a
 * plain parameter, keeping this class independent of persistence.
 */
@Component
public class LeaseQuoteCalculator {

    private static final BigDecimal VAT_DIVISOR = new BigDecimal("1.23");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final int MONEY_SCALE = 2;
    private static final MathContext MATH_CONTEXT = new MathContext(20, RoundingMode.HALF_UP);

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
     * @param initialPaymentPercent  within the selected product's allowed range
     * @param buyoutPercent          within the selected product's allowed range
     * @param termMonths             one of the selected product's offered terms
     * @param annualRatePercent      resolved from the selected product's lease-type configuration
     */
    public Result calculate(
            BigDecimal vehiclePricePln,
            BigDecimal initialPaymentPercent,
            BigDecimal buyoutPercent,
            int termMonths,
            BigDecimal annualRatePercent
    ) {
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
