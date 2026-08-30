package com.leasedemo.lease.product.exception;

/** Thrown when a lease quote request references an unknown {@code productCode}. */
public class LeaseProductNotFoundException extends RuntimeException {

    public LeaseProductNotFoundException(String message) {
        super(message);
    }
}
