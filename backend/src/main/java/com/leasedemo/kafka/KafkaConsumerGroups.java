package com.leasedemo.kafka;

/**
 * Central registry of Kafka consumer group ids used by the LeaseDemo
 * backend (M5.5 §19).
 */
public final class KafkaConsumerGroups {

    /**
     * Consumer group for the PDF-generation + email-sending pipeline
     * reacting to {@code LeaseApplicationApprovedEvent}.
     */
    public static final String LEASE_PDF_MAIL = "leasedemo-pdf-mail-v1";

    private KafkaConsumerGroups() {
    }
}
