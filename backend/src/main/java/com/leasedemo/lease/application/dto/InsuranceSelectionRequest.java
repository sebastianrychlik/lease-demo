package com.leasedemo.lease.application.dto;

import com.leasedemo.lease.application.insurance.InsuranceCoverageCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * A single selected insurance coverage submitted as part of
 * {@link CreateLeaseApplicationRequest} (M5.3 §4).
 *
 * <p>Only enabled/selected coverages are submitted — disabled coverages are
 * simply omitted from the list. {@code monthlyPremium} is intentionally
 * absent: the backend independently computes and validates the premium via
 * {@code InsurancePremiumCalculator} (M5.3 §10, §38).
 */
public record InsuranceSelectionRequest(
        @NotNull
        @Schema(description = "Insurance coverage code", example = "GAP")
        InsuranceCoverageCode code,

        @Schema(description = "Coverage option (null for coverages without options)", example = "PREMIUM")
        String option
) {
}
