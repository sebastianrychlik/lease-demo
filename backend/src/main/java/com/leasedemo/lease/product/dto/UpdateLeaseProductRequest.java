package com.leasedemo.lease.product.dto;

import com.leasedemo.lease.quote.model.LeaseCurrency;
import com.leasedemo.lease.quote.model.LeaseType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * Inbound payload for {@code PUT /api/admin/lease-products/{code}}
 * (M5.1.4 §10).
 *
 * <p>Deliberately has NO {@code code} field — product code is stable
 * identity, established at creation and never changed via update (§10-11).
 * The path variable is the sole source of which product is being updated.
 */
public record UpdateLeaseProductRequest(
        @NotNull
        String name,

        @NotNull
        String market,

        @NotNull
        Boolean enabled,

        LocalDate validFrom,
        LocalDate validTo,

        @NotEmpty
        List<LeaseCurrency> currencies,

        @NotNull
        LeaseCurrency settlementCurrency,

        @NotNull
        LeaseCurrency defaultCurrency,

        @NotEmpty
        List<Integer> terms,

        @NotNull
        Integer defaultTermMonths,

        @Valid
        @NotNull
        PercentageRangeDto initialPayment,

        @Valid
        @NotNull
        PercentageRangeDto buyout,

        @NotEmpty
        List<LeaseTypeOptionDto> leaseTypes,

        @NotNull
        LeaseType defaultLeaseType
) {
}
