package com.leasedemo.exception;

/**
 * Thrown when attempting to register a customer whose PESEL (identified via
 * its {@code pesel_lookup} hash) already exists, or whose authenticated
 * Keycloak identity ({@code keycloak_user_id}) is already registered to an
 * existing customer.
 */
public class DuplicateCustomerException extends RuntimeException {

    public DuplicateCustomerException(String message) {
        super(message);
    }
}
