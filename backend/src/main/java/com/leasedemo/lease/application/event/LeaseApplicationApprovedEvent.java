package com.leasedemo.lease.application.event;

import com.leasedemo.lease.application.model.ApplicationStatus;
import com.leasedemo.lease.quote.model.LeaseCurrency;

import java.time.Instant;
import java.util.UUID;

/**
 * Kafka event payload published when a {@code LeaseApplication} is
 * persisted with {@code status == APPROVED} (M5.5 §7).
 *
 * <p>Deliberately small — this is a clean event DTO, never the JPA
 * {@code LeaseApplication} entity (M5.5 §44). PostgreSQL remains the
 * authoritative business snapshot; the consumer reloads it using
 * {@link #applicationId()} rather than trusting a payload copy embedded in
 * Kafka.
 *
 * <p>{@code eventVersion} is fixed at {@code 1} and the topic name itself
 * is versioned ({@code lease.application.approved.v1}) — together these
 * give a simple, explicit basis for future event-schema evolution
 * (M5.5 §8).
 */
public record LeaseApplicationApprovedEvent(
        UUID eventId,
        int eventVersion,
        Instant occurredAt,
        UUID applicationId,
        ApplicationStatus status,
        String productCode,
        LeaseCurrency settlementCurrency
) {

    public static final int CURRENT_VERSION = 1;

    public static LeaseApplicationApprovedEvent of(
            UUID applicationId, ApplicationStatus status, String productCode, LeaseCurrency settlementCurrency) {
        return new LeaseApplicationApprovedEvent(
                UUID.randomUUID(), CURRENT_VERSION, Instant.now(), applicationId, status, productCode, settlementCurrency);
    }
}
