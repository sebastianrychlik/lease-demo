package com.leasedemo.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the Kafka topics owned by this application using Spring Kafka's
 * standard {@link TopicBuilder}/{@link NewTopic} support (M5.5 §6).
 *
 * <p>Spring Boot's {@code KafkaAdmin} auto-configuration creates any
 * {@link NewTopic} bean on startup if it does not already exist on the
 * broker. This is sufficient for the local single-broker demo environment
 * — no cluster-level Kafka administration is implemented here.
 */
@Configuration
public class KafkaTopicConfig {

    /**
     * Local demo configuration: 3 partitions, replication factor 1 (the
     * local environment runs exactly one Kafka broker).
     */
    @Bean
    public NewTopic leaseApplicationApprovedTopic() {
        return TopicBuilder.name(KafkaTopicNames.LEASE_APPLICATION_APPROVED)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
