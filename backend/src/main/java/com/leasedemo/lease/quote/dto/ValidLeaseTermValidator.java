package com.leasedemo.lease.quote.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;

/**
 * Validates that a lease term (months) belongs to the M5.1 supported set.
 *
 * <p>{@code null} is considered valid here — use {@code @NotNull} alongside
 * this annotation to reject missing values with a dedicated message.
 */
public class ValidLeaseTermValidator implements ConstraintValidator<ValidLeaseTerm, Integer> {

    private static final Set<Integer> SUPPORTED_TERMS = Set.of(24, 36, 48, 60);

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        return value == null || SUPPORTED_TERMS.contains(value);
    }
}
