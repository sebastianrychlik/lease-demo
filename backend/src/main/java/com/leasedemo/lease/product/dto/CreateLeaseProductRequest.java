package com.leasedemo.lease.product.dto;

import com.leasedemo.lease.quote.model.LeaseCurrency;
import com.leasedemo.lease.quote.model.LeaseType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.List;

/**
 * Inbound payload for {@code POST /api/admin/lease-products} (M5.1.4 §9).
 *
 * <p>Structural validation ({@code @NotNull}/{@code @NotEmpty}/format) stays
 * here; cross-field BUSINESS validation (settlement/default currency must
 * belong to {@code currencies}, default term must belong to {@code terms},
 * range/APR sanity, etc. — §17-22) is performed authoritatively by
 * {@code AdminLeaseProductService}.
 */
public record CreateLeaseProductRequest(
        @NotNull
        @Pattern(regexp = "^[A-Z0-9_]+$", message = "code must match ^[A-Z0-9_]+$")
        String code,

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
