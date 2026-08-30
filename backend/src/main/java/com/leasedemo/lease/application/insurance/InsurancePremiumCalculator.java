package com.leasedemo.lease.application.insurance;

import com.leasedemo.lease.application.dto.InsuranceCoverageSnapshot;
import com.leasedemo.lease.application.dto.InsuranceSelectionRequest;
import com.leasedemo.lease.application.exception.InvalidInsuranceConfigurationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Small, pure, backend-owned insurance validation + pricing engine
 * (M5.3 §9, §10, §11).
 *
 * <p>No database, no ADMIN configuration — mirrors the M5.2 Angular demo
 * premium table exactly, but is the sole AUTHORITATIVE source once
 * persistence is involved (M5.3 §38): the browser may display these same
 * numbers for UX, but never supplies them to the backend.
 */
@Component
public class InsurancePremiumCalculator {

    /** GAP coverage premiums by option — no option-less GAP selection is valid. */
    private static final Map<String, BigDecimal> GAP_OPTIONS = Map.of(
            "STANDARD", new BigDecimal("79"),
            "PREMIUM", new BigDecimal("109")
    );

    /** Assistance coverage premiums by option. */
    private static final Map<String, BigDecimal> ASSISTANCE_OPTIONS = Map.of(
            "POLAND", new BigDecimal("29"),
            "EUROPE", new BigDecimal("49")
    );

    /** Replacement Car has no option — flat premium. */
    private static final BigDecimal REPLACEMENT_CAR_PREMIUM = new BigDecimal("39");

    /**
     * Validates and prices the submitted coverage selections.
     *
     * @param selections the CUSTOMER-submitted, ENABLED-only coverage selections;
     *                    may be {@code null}/empty — no insurance is valid (M5.3 §30)
     * @throws InvalidInsuranceConfigurationException on unknown code/option or duplicates
     */
    public Result calculate(List<InsuranceSelectionRequest> selections) {
        if (selections == null || selections.isEmpty()) {
            return new Result(List.of(), BigDecimal.ZERO);
        }

        EnumSet<InsuranceCoverageCode> seen = EnumSet.noneOf(InsuranceCoverageCode.class);
        Map<InsuranceCoverageCode, InsuranceCoverageSnapshot> snapshots = new EnumMap<>(InsuranceCoverageCode.class);

        for (InsuranceSelectionRequest selection : selections) {
            InsuranceCoverageCode code = selection.code();
            if (code == null) {
                throw new InvalidInsuranceConfigurationException("Insurance coverage code is required");
            }
            if (!seen.add(code)) {
                throw new InvalidInsuranceConfigurationException(
                        "Duplicate insurance coverage code: " + code);
            }

            BigDecimal premium = switch (code) {
                case GAP -> premiumForOption(code, GAP_OPTIONS, selection.option());
                case ASSISTANCE -> premiumForOption(code, ASSISTANCE_OPTIONS, selection.option());
                case REPLACEMENT_CAR -> {
                    if (selection.option() != null) {
                        throw new InvalidInsuranceConfigurationException(
                                "Coverage REPLACEMENT_CAR does not accept an option: " + selection.option());
                    }
                    yield REPLACEMENT_CAR_PREMIUM;
                }
            };

            snapshots.put(code, new InsuranceCoverageSnapshot(code, selection.option(), premium));
        }

        BigDecimal total = snapshots.values().stream()
                .map(InsuranceCoverageSnapshot::monthlyPremium)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new Result(List.copyOf(snapshots.values()), total);
    }

    private BigDecimal premiumForOption(InsuranceCoverageCode code, Map<String, BigDecimal> options, String option) {
        if (option == null) {
            throw new InvalidInsuranceConfigurationException("Coverage " + code + " requires an option");
        }
        BigDecimal premium = options.get(option);
        if (premium == null) {
            throw new InvalidInsuranceConfigurationException(
                    "Invalid option '" + option + "' for coverage " + code);
        }
        return premium;
    }

    /** Result of insurance validation/pricing — snapshot list + total monthly premium. */
    public record Result(List<InsuranceCoverageSnapshot> coverages, BigDecimal totalMonthlyPremium) {
    }
}
