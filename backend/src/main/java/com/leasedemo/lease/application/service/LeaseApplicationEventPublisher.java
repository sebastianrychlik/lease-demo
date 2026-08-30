package com.leasedemo.lease.application.service;

import com.leasedemo.kafka.KafkaTopicNames;
import com.leasedemo.lease.application.event.LeaseApplicationApprovedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Dedicated Kafka producer boundary for {@link LeaseApplicationApprovedEvent}
 * (M5.5 §11).
 *
 * <p>Kept as its own bean so {@code KafkaTemplate} never leaks into
 * {@code LeaseApplicationController} or is called before the owning
 * {@code LeaseApplication} has been durably persisted.
 *
 * <p><strong>Known limitation (M5.5 §15, §58):</strong> this is a direct
 * post-commit publish, NOT a Transactional Outbox. A process crash exactly
 * between the database commit and this Kafka send could leave an APPROVED
 * application without a corresponding event. The production-grade
 * evolution of this is Transactional Outbox + Debezium CDC — intentionally
 * NOT implemented in this speed milestone.
 */
@Component
public class LeaseApplicationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LeaseApplicationEventPublisher.class);

    private final KafkaTemplate<String, LeaseApplicationApprovedEvent> kafkaTemplate;

    public LeaseApplicationEventPublisher(KafkaTemplate<String, LeaseApplicationApprovedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publishes asynchronously — never blocks the CUSTOMER HTTP thread
     * waiting for a Kafka broker acknowledgement (M5.5 §16, §37). The Kafka
     * message key is the {@code applicationId}, so events for the same
     * application retain partition ordering (M5.5 §10).
     */
    public void publishApproved(LeaseApplicationApprovedEvent event) {
        String key = event.applicationId().toString();

        kafkaTemplate.send(KafkaTopicNames.LEASE_APPLICATION_APPROVED, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("event publishing failed: eventId={}, applicationId={}",
                                event.eventId(), event.applicationId(), ex);
                    } else {
                        log.info("event published successfully: eventId={}, applicationId={}, topic={}, partition={}",
                                event.eventId(), event.applicationId(),
                                result.getRecordMetadata().topic(), result.getRecordMetadata().partition());
                    }
                });
    }
}
