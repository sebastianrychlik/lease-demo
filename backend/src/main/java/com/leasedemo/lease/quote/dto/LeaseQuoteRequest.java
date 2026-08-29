package com.leasedemo.lease.quote.dto;

import com.leasedemo.lease.quote.model.LeaseCurrency;
import com.leasedemo.lease.quote.model.LeaseType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Inbound payload for {@code POST /api/lease-quotes/calculate}.
 *
 * <p>This is untrusted client-supplied input. The backend is the sole
 * authority on the lease calculation — Angular never computes or
 * duplicates the formula, it only renders {@link LeaseQuoteResponse}.
 *
 * <p>Structural/format validation (types, {@code @NotNull}) stays here.
 * Business validation (which currencies/terms/ranges/lease types are
 * actually offered) is delegated to the selected {@code productCode} via
 * {@code LeaseProductService} (M5.1.2) — hard-coded ranges/term sets are no
 * longer embedded in bean validation constraints.
 */
public record LeaseQuoteRequest(
        @NotNull
        @Schema(description = "Selected Lease Product code", example = "STANDARD_CAR_PL")
        String productCode,

        @NotNull
        @Positive
        @Schema(description = "Vehicle price in the request currency", example = "45000")
        BigDecimal vehiclePrice,

        @NotNull
        @Schema(description = "Currency of vehiclePrice — must be offered by the selected product", example = "EUR")
        LeaseCurrency currency,

        @NotNull
        @Schema(description = "Lease term in months — must be offered by the selected product", example = "36")
        Integer termMonths,

        @NotNull
        @Schema(description = "Initial payment as a percentage of vehicle price — must be within the selected product's range", example = "20")
        BigDecimal initialPaymentPercent,

        @NotNull
        @Schema(description = "Buyout/residual value as a percentage of vehicle price — must be within the selected product's range", example = "15")
        BigDecimal buyoutPercent,

        @NotNull
        @Schema(description = "Lease type — must be offered by the selected product", example = "OPERATING")
        LeaseType leaseType
) {
}
