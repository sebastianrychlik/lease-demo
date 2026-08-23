package com.leasedemo.exception;

/**
 * Thrown when attempting to register a customer whose PESEL (identified via
 * its {@code pesel_lookup} hash) already exists.
 */
public class DuplicateCustomerException extends RuntimeException {

    public DuplicateCustomerException(String message) {
        super(message);
    }
}
