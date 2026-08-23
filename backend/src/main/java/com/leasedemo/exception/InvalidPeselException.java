package com.leasedemo.exception;

/**
 * Thrown when a supplied PESEL fails structural validation (format,
 * checksum) or is inconsistent with the declared date of birth / gender.
 */
public class InvalidPeselException extends RuntimeException {

    public InvalidPeselException(String message) {
        super(message);
    }
}
