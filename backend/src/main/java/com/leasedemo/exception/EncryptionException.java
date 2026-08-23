package com.leasedemo.exception;

/**
 * Thrown when application-level field encryption or decryption fails.
 *
 * <p>Wraps low-level {@code javax.crypto} checked exceptions so that
 * callers do not need to handle cryptography-specific checked exception
 * types directly.
 */
public class EncryptionException extends RuntimeException {

    public EncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
