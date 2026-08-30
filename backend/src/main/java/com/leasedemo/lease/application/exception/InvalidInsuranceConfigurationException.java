package com.leasedemo.lease.application.exception;

/**
 * Thrown when a submitted insurance selection references an unknown
 * coverage code, an invalid option for that coverage, or a duplicate
 * coverage code (M5.3 §10).
 */
public class InvalidInsuranceConfigurationException extends RuntimeException {

    public InvalidInsuranceConfigurationException(String message) {
        super(message);
    }
}
