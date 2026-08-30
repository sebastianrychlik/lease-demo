package com.leasedemo.lease.application.exception;

/**
 * Thrown when the submitted financial application input
 * (monthlyNetIncome/monthlyObligations) fails backend validation (M5.3 §15).
 */
public final class InvalidApplicationInputException extends RuntimeException {

    public InvalidApplicationInputException(String message) {
        super(message);
    }
}
