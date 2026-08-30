package com.leasedemo.lease.application.model;

/**
 * Decision outcome of a {@code LeaseApplication} (M5.3).
 *
 * <p>Intentionally small — a full workflow state machine is out of scope
 * for this milestone. {@code APPROVED} is the first scenario M5.5 will use
 * to publish a Kafka event.
 */
public enum ApplicationStatus {
    APPROVED,
    REVIEW,
    REJECTED
}
