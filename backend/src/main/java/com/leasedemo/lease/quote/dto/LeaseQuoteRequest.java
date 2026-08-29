package com.leasedemo.lease.quote.dto;

import com.leasedemo.lease.quote.model.LeaseCurrency;
import com.leasedemo.lease.quote.model.LeaseType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Inbound payload for {@code POST /api/lease-quotes/calculate}.
 *
 * <p>This is untrusted client-supplied input. The backend is the sole
 * authority on the lease calculation — Angular never computes or
 * duplicates the formula, it only renders {@link LeaseQuoteResponse}.
 */
public record LeaseQuoteRequest(
        @NotNull
        @Positive
        @Schema(description = "Vehicle price in the request currency", example = "45000")
        BigDecimal vehiclePrice,

        @NotNull
        @Schema(description = "Currency of vehiclePrice. Supported: PLN, EUR", example = "EUR")
        LeaseCurrency currency,

        @NotNull
        @ValidLeaseTerm
        @Schema(description = "Lease term in months. Supported: 24, 36, 48, 60", example = "36")
        Integer termMonths,

        @NotNull
        @DecimalMin(value = "0", inclusive = true)
        @DecimalMax(value = "45", inclusive = true)
        @Schema(description = "Initial payment as a percentage of vehicle price (0-45)", example = "20")
        BigDecimal initialPaymentPercent,

        @NotNull
        @DecimalMin(value = "1", inclusive = true)
        @DecimalMax(value = "40", inclusive = true)
        @Schema(description = "Buyout/residual value as a percentage of vehicle price (1-40)", example = "15")
        BigDecimal buyoutPercent,

        @NotNull
        @Schema(description = "Lease type. Supported: OPERATING, FINANCIAL", example = "OPERATING")
        LeaseType leaseType
) {
}
