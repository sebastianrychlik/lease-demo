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
            BigDecimal vehiclePrice,
            BigDecimal initialPayment,
            BigDecimal buyout,
            BigDecimal financedAmount,
            BigDecimal monthlyPayment,
            BigDecimal totalLeaseCost,
            BigDecimal estimatedVat
    ) {
    }

    /**
     * Runs the demo lease calculation described in the M5.1 specification.
     *
     * <p>Currency-neutral (M5.1.3.2): every amount here is expressed in a
     * single settlement currency already resolved by the caller — this
     * class has no knowledge of which currency that is, NBP, JPA, or HTTP.
     *
     * @param vehiclePrice           vehicle price already converted to the settlement currency
     * @param initialPaymentPercent  within the selected product's allowed range
     * @param buyoutPercent          within the selected product's allowed range
     * @param termMonths             one of the selected product's offered terms
     * @param annualRatePercent      resolved from the selected product's lease-type configuration
     */
    public Result calculate(
            BigDecimal vehiclePrice,
            BigDecimal initialPaymentPercent,
            BigDecimal buyoutPercent,
            int termMonths,
            BigDecimal annualRatePercent
    ) {
        BigDecimal initialPayment = vehiclePrice
                .multiply(initialPaymentPercent, MATH_CONTEXT)
                .divide(ONE_HUNDRED, MATH_CONTEXT);

        BigDecimal buyout = vehiclePrice
                .multiply(buyoutPercent, MATH_CONTEXT)
                .divide(ONE_HUNDRED, MATH_CONTEXT);

        BigDecimal amountAfterInitialPayment = vehiclePrice.subtract(initialPayment);

        BigDecimal monthlyRate = annualRatePercent
                .divide(new BigDecimal("12"), MATH_CONTEXT)
                .divide(ONE_HUNDRED, MATH_CONTEXT);

        BigDecimal onePlusMonthlyRate = BigDecimal.ONE.add(monthlyRate, MATH_CONTEXT);
        BigDecimal growthFactor = onePlusMonthlyRate.pow(termMonths, MATH_CONTEXT);

        BigDecimal presentValueOfBuyout = buyout.divide(growthFactor, MATH_CONTEXT);

        BigDecimal amortizedAmount = amountAfterInitialPayment.subtract(presentValueOfBuyout);
        if (amortizedAmount.signum() < 0) {
            // Guard against pathological input combinations (e.g. buyout so high the
            // present value already exceeds the financed amount) producing a negative
            // amortized base, which would otherwise flip the sign of monthlyPayment.
            amortizedAmount = BigDecimal.ZERO;
        }

        BigDecimal negativeExponentFactor = BigDecimal.ONE.divide(growthFactor, MATH_CONTEXT);
        BigDecimal denominator = BigDecimal.ONE.subtract(negativeExponentFactor, MATH_CONTEXT);

        BigDecimal computedMonthlyPayment;
        if (denominator.compareTo(BigDecimal.ZERO) <= 0) {
            // Defensive fallback — should not occur for a positive monthlyRate/termMonths,
            // but protects the calculation from division by zero / negative results.
            computedMonthlyPayment = BigDecimal.ZERO;
        } else {
            computedMonthlyPayment = amortizedAmount
                    .multiply(monthlyRate, MATH_CONTEXT)
                    .divide(denominator, MATH_CONTEXT);
        }
        if (computedMonthlyPayment.signum() < 0) {
            computedMonthlyPayment = BigDecimal.ZERO;
        }

        BigDecimal computedTotalLeaseCost = initialPayment
                .add(computedMonthlyPayment.multiply(BigDecimal.valueOf(termMonths), MATH_CONTEXT))
                .add(buyout);

        BigDecimal computedEstimatedVat = computedTotalLeaseCost.subtract(computedTotalLeaseCost.divide(VAT_DIVISOR, MATH_CONTEXT));

        return new Result(
                round(vehiclePrice),
                round(initialPayment),
                round(buyout),
                round(amountAfterInitialPayment),
                round(computedMonthlyPayment),
                round(computedTotalLeaseCost),
                round(computedEstimatedVat)
        );
    }

    private static BigDecimal round(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
