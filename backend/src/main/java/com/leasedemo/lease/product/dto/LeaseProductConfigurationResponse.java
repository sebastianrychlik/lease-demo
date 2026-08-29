package com.leasedemo.lease.product.dto;

import com.leasedemo.lease.quote.model.LeaseCurrency;
import com.leasedemo.lease.quote.model.LeaseType;

import java.util.List;

/**
 * Frontend-oriented Lease Product configuration DTO returned by
 * {@code GET /api/lease-products/available}.
 *
 * <p>Angular renders its Reactive Form entirely from this shape — it never
 * hard-codes currencies, terms, ranges, or lease types.
 */
public record LeaseProductConfigurationResponse(
        String code,
        String name,
        String market,

        List<LeaseCurrency> currencies,
        LeaseCurrency settlementCurrency,
        List<Integer> termsMonths,

        PercentageRangeDto initialPayment,
        PercentageRangeDto buyout,

        List<LeaseTypeOptionDto> leaseTypes,

        LeaseCurrency defaultCurrency,
        Integer defaultTermMonths,
        LeaseType defaultLeaseType
) {
}
