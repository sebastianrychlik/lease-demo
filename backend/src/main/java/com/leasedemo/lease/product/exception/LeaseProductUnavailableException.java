package com.leasedemo.lease.product.exception;

/**
 * Thrown when a requested lease product exists but is not currently
 * usable — disabled, wrong market, or outside its valid-from/valid-to
 * window.
 */
public class LeaseProductUnavailableException extends RuntimeException {

    public LeaseProductUnavailableException(String message) {
        super(message);
    }
}
