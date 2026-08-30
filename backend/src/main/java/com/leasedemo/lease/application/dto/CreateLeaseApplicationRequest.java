package com.leasedemo.lease.application.dto;

import com.leasedemo.lease.quote.model.LeaseCurrency;
import com.leasedemo.lease.quote.model.LeaseType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

/**
 * Inbound payload for {@code POST /api/lease-applications} (M5.3 §4).
 *
 * <p>This is untrusted client-supplied INPUT only. The backend recalculates
 * the authoritative {@code LeaseQuoteResponse} via the existing
 * {@code LeaseQuoteService} (M5.3 §3) rather than trusting any
 * monthlyPayment/exchangeRate/financedAmount/totalLeaseCost value the
 * browser might supply — this DTO deliberately has no such fields.
 *
 * <p>Customer identity is never part of this request — it is resolved
 * exclusively from the authenticated JWT {@code sub} claim (M5.3 §5).
 */
public record CreateLeaseApplicationRequest(
        @NotNull
        @Schema(description = "Selected Lease Product code", example = "STANDARD_CAR_PL")
        String productCode,

        @NotNull
        @Positive
        @Schema(description = "Vehicle price in the request currency", example = "45000")
        BigDecimal vehiclePrice,

        @NotNull
        @Schema(description = "Currency of vehiclePrice", example = "EUR")
        LeaseCurrency vehiclePriceCurrency,

        @NotNull
        @Schema(description = "Lease term in months", example = "36")
        Integer termMonths,

        @NotNull
        @Schema(description = "Initial payment as a percentage of vehicle price", example = "20")
        BigDecimal initialPaymentPercent,

        @NotNull
        @Schema(description = "Buyout/residual value as a percentage of vehicle price", example = "15")
        BigDecimal buyoutPercent,

        @NotNull
        @Schema(description = "Lease type", example = "OPERATING")
        LeaseType leaseType,

        @Valid
        @Schema(description = "Only selected/enabled insurance coverages — omit disabled coverages")
        List<InsuranceSelectionRequest> insurance,

        @NotNull
        @Positive
        @Schema(description = "Monthly net income of the applicant", example = "15000")
        BigDecimal monthlyNetIncome,

        @NotNull
        @PositiveOrZero
        @Schema(description = "Monthly financial obligations of the applicant", example = "1000")
        BigDecimal monthlyObligations
) {
}
