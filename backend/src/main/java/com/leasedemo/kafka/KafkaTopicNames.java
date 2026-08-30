package com.leasedemo.kafka;

/**
 * Central registry of Kafka topic names used by the LeaseDemo backend
 * (M5.5).
 *
 * <p>Topic names are versioned ({@code .v1} suffix) so a future breaking
 * change to the event schema can be introduced as a new topic
 * ({@code .v2}) without silently changing the contract of this one — a
 * simple, interview-friendly approach to event schema evolution.
 *
 * <p>No raw topic-name string literals should appear anywhere else in the
 * codebase — always reference these constants.
 */
public final class KafkaTopicNames {

    /**
     * Published exactly once, after a {@code LeaseApplication} has been
     * durably persisted with {@code status == APPROVED} (M5.5 §5, §12).
     */
    public static final String LEASE_APPLICATION_APPROVED = "lease.application.approved.v1";

    private KafkaTopicNames() {
    }
}
