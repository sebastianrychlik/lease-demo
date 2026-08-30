package com.leasedemo.lease.application.insurance;

/**
 * Backend-owned insurance coverage codes (M5.3 §9).
 *
 * <p>Mirrors the M5.2 frontend demo coverages, but the backend never trusts
 * the browser's premium — {@link InsurancePremiumCalculator} is the sole
 * authority for validation and pricing.
 */
public enum InsuranceCoverageCode {
    GAP,
    ASSISTANCE,
    REPLACEMENT_CAR
}
