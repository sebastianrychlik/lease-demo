package com.leasedemo.lease.quote.exception;

/**
 * Thrown when a lease quote is requested for a currency that is not
 * present in the current NBP Table A response (should not normally happen
 * for the M5.1 supported currency set, but guards against upstream data
 * gaps).
 */
public class UnsupportedCurrencyException extends RuntimeException {

    public UnsupportedCurrencyException(String message) {
        super(message);
    }
}
