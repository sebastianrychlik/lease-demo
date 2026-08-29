package com.leasedemo.exception;

/**
 * Thrown when an authenticated JWT subject has no associated Customer
 * domain profile yet.
 *
 * <p>This is an expected, non-error condition for a freshly authenticated
 * Keycloak CUSTOMER who has not yet completed onboarding — {@code
 * GlobalExceptionHandler} maps it to {@code 404 Not Found} so Angular can
 * distinguish "authenticated, no profile" (start onboarding) from any
 * other failure.
 */
public class CustomerProfileNotFoundException extends RuntimeException {

    public CustomerProfileNotFoundException(String message) {
        super(message);
    }
}
