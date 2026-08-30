package com.leasedemo.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Small, reasonable Kafka listener error strategy (M5.5 §32): 2 retries at a
 * 1 second interval, then log ERROR (already done in
 * {@code LeaseApplicationApprovedConsumer}) and give up.
 *
 * <p>Intentionally NOT a DLQ / retry-topic / exponential-backoff setup —
 * out of scope for this speed milestone.
 */
@Configuration
public class KafkaConsumerErrorHandlingConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerErrorHandlingConfig.class);

    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        // 2 retries, 1000ms apart (3 attempts total).
        FixedBackOff backOff = new FixedBackOff(1000L, 2L);
        DefaultErrorHandler handler = new DefaultErrorHandler(
                (record, exception) -> log.error(
                        "listener processing ultimately failed after retries: topic={}, partition={}, offset={}",
                        record.topic(), record.partition(), record.offset(), exception),
                backOff);
        return handler;
    }
}
