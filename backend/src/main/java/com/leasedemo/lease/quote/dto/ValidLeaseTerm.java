package com.leasedemo.lease.quote.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a lease term (in months) is one of the M5.1 supported
 * terms: 24, 36, 48, or 60.
 */
@Documented
@Constraint(validatedBy = ValidLeaseTermValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidLeaseTerm {
    String message() default "termMonths must be one of 24, 36, 48, or 60";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
