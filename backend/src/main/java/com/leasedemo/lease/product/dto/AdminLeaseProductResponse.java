package com.leasedemo.lease.product.dto;

import com.leasedemo.lease.quote.model.LeaseCurrency;
import com.leasedemo.lease.quote.model.LeaseType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * ADMIN-oriented Lease Product configuration DTO (M5.1.4).
 *
 * <p>Returned by {@code GET /api/admin/lease-products} and
 * {@code GET /api/admin/lease-products/{code}}. Distinct from
 * {@link LeaseProductConfigurationResponse} (the CUSTOMER-facing shape) —
 * this one additionally exposes {@code enabled}, {@code validFrom}/
 * {@code validTo}, and audit timestamps, none of which the CUSTOMER
 * endpoint needs. The {@code LeaseProduct} JPA entity is never exposed
 * directly through the controller.
 */
public record AdminLeaseProductResponse(
        String code,
        String name,
        String market,
        boolean enabled,

        LocalDate validFrom,
        LocalDate validTo,

        List<LeaseCurrency> currencies,
        LeaseCurrency settlementCurrency,
        LeaseCurrency defaultCurrency,

        List<Integer> termsMonths,
        Integer defaultTermMonths,

        PercentageRangeDto initialPayment,
        PercentageRangeDto buyout,

        List<LeaseTypeOptionDto> leaseTypes,
        LeaseType defaultLeaseType,

        Instant createdAt,
        Instant updatedAt
) {
}
