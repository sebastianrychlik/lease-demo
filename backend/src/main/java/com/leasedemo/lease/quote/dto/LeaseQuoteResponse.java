package com.leasedemo.lease.quote.dto;

import com.leasedemo.lease.quote.model.LeaseCurrency;
import com.leasedemo.lease.quote.model.LeaseType;

import java.math.BigDecimal;

/**
 * Outbound payload for {@code POST /api/lease-quotes/calculate}.
 *
 * <p>Carries every value Angular needs to render the quote summary so the
 * frontend remains presentation-only and never re-derives the calculation.
 */
public record LeaseQuoteResponse(
        BigDecimal vehiclePriceOriginal,
        LeaseCurrency currency,
        BigDecimal exchangeRate,
        /** Effective date of the NBP rate used, or {@code null} for PLN (no NBP lookup performed). */
        String exchangeRateDate,
        BigDecimal vehiclePricePln,

        Integer termMonths,

        BigDecimal initialPaymentPercent,
        BigDecimal initialPaymentPln,

        BigDecimal buyoutPercent,
        BigDecimal buyoutPln,

        LeaseType leaseType,
        BigDecimal annualRatePercent,

        BigDecimal financedAmountPln,
        BigDecimal monthlyPaymentPln,
        BigDecimal totalLeaseCostPln,
        BigDecimal estimatedVatPln
) {
}
