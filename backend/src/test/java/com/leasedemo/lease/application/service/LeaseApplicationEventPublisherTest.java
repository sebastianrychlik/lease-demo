package com.leasedemo.lease.application.service;

import com.leasedemo.kafka.KafkaTopicNames;
import com.leasedemo.lease.application.event.LeaseApplicationApprovedEvent;
import com.leasedemo.lease.application.model.ApplicationStatus;
import com.leasedemo.lease.quote.model.LeaseCurrency;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaseApplicationEventPublisher")
class LeaseApplicationEventPublisherTest {

    @Mock
    private KafkaTemplate<String, LeaseApplicationApprovedEvent> kafkaTemplate;

    private LeaseApplicationEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new LeaseApplicationEventPublisher(kafkaTemplate);
    }

    @Test
    @DisplayName("sends to the versioned topic, keyed by applicationId, with eventVersion == 1 (M5.5)")
    void publishesWithCorrectTopicKeyAndEvent() {
        UUID applicationId = UUID.randomUUID();
        LeaseApplicationApprovedEvent event = LeaseApplicationApprovedEvent.of(
                applicationId, ApplicationStatus.APPROVED, "STANDARD_CAR_PL", LeaseCurrency.EUR);

        SendResult<String, LeaseApplicationApprovedEvent> sendResult =
                new SendResult<>(null, new RecordMetadata(new TopicPartition(KafkaTopicNames.LEASE_APPLICATION_APPROVED, 0), 0, 0, 0, 0, 0));
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        publisher.publishApproved(event);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LeaseApplicationApprovedEvent> eventCaptor = ArgumentCaptor.forClass(LeaseApplicationApprovedEvent.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo(KafkaTopicNames.LEASE_APPLICATION_APPROVED);
        assertThat(keyCaptor.getValue()).isEqualTo(applicationId.toString());
        assertThat(eventCaptor.getValue().applicationId()).isEqualTo(applicationId);
        assertThat(eventCaptor.getValue().eventVersion()).isEqualTo(1);
    }
}
