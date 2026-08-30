package com.leasedemo.lease.application.dto;

import com.leasedemo.lease.application.model.ApplicationStatus;
import com.leasedemo.lease.quote.model.LeaseCurrency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Outbound payload for {@code POST /api/lease-applications} (M5.3 §17).
 *
 * <p>{@code estimatedMonthlyTotal = monthlyPayment + insuranceMonthlyPremium},
 * computed once server-side so Angular never re-derives it.
 */
public record LeaseApplicationResponse(
        UUID applicationId,
        ApplicationStatus status,
        Integer creditScore,
        Instant submittedAt,

        String productCode,
        String productName,

        LeaseCurrency settlementCurrency,

        BigDecimal monthlyPayment,
        BigDecimal insuranceMonthlyPremium,
        BigDecimal estimatedMonthlyTotal
) {
}
