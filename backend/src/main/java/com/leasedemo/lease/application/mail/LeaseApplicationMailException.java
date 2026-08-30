package com.leasedemo.lease.application.mail;

/**
 * Wraps checked mail-sending failures (M5.5 §28) so the Kafka consumer can
 * catch a single unchecked type and log a clear ERROR with
 * eventId/applicationId (M5.5 §32) without swallowing the failure.
 */
public class LeaseApplicationMailException extends RuntimeException {

    public LeaseApplicationMailException(String message, Throwable cause) {
        super(message, cause);
    }
}
