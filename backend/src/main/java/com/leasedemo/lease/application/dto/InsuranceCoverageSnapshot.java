package com.leasedemo.lease.application.dto;

import com.leasedemo.lease.application.insurance.InsuranceCoverageCode;

import java.math.BigDecimal;

/**
 * One validated, backend-priced coverage entry persisted inside
 * {@code LeaseApplication.insuranceConfiguration} JSONB (M5.3 §11).
 *
 * <p>Serialized as-is by Jackson/Hibernate 6 JSON mapping — no manual JSON
 * string concatenation.
 */
public record InsuranceCoverageSnapshot(
        InsuranceCoverageCode code,
        String option,
        BigDecimal monthlyPremium
) {
}
