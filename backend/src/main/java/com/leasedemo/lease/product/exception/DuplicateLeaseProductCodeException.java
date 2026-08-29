package com.leasedemo.lease.product.exception;

/**
 * Thrown when ADMIN attempts to create a Lease Product whose {@code code}
 * already exists (M5.1.4 §11). Product code must be unique/stable.
 */
public class DuplicateLeaseProductCodeException extends RuntimeException {

    public DuplicateLeaseProductCodeException(String message) {
        super(message);
    }
}
