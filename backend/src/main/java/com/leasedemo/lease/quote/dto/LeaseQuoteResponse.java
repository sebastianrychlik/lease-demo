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
        String productCode,
        String productName,

        BigDecimal vehiclePriceOriginal,
        LeaseCurrency vehiclePriceCurrency,

        /** Resolved from the selected {@code LeaseProduct} — never client-supplied (M5.1.3.2). */
        LeaseCurrency settlementCurrency,

        /**
         * Amount of {@code settlementCurrency} for ONE unit of
         * {@code vehiclePriceCurrency}. {@code 1} when
         * {@code vehiclePriceCurrency == settlementCurrency}.
         */
        BigDecimal exchangeRate,
        /** Effective date of the NBP rate used, or {@code null} when no NBP lookup was performed (same-currency). */
        String exchangeRateDate,
        BigDecimal vehiclePriceSettlement,

        Integer termMonths,

        BigDecimal initialPaymentPercent,
        BigDecimal initialPayment,

        BigDecimal buyoutPercent,
        BigDecimal buyout,

        LeaseType leaseType,
        BigDecimal annualRatePercent,

        BigDecimal financedAmount,
        BigDecimal monthlyPayment,
        BigDecimal totalLeaseCost,
        BigDecimal estimatedVat
) {
}
