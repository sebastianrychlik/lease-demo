package com.leasedemo.lease.application.consumer;

/**
 * Raised when the Kafka consumer cannot locate the persisted
 * {@code LeaseApplication} referenced by an incoming
 * {@code LeaseApplicationApprovedEvent} (M5.5 §21, §32).
 */
public class LeaseApplicationConsumerException extends RuntimeException {

    public LeaseApplicationConsumerException(String message) {
        super(message);
    }
}
