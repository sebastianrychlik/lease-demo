package com.leasedemo.lease.application.consumer;

import com.leasedemo.kafka.KafkaConsumerGroups;
import com.leasedemo.kafka.KafkaTopicNames;
import com.leasedemo.lease.application.entity.LeaseApplication;
import com.leasedemo.lease.application.event.LeaseApplicationApprovedEvent;
import com.leasedemo.lease.application.mail.LeaseApplicationMailService;
import com.leasedemo.lease.application.pdf.LeaseApplicationPdfService;
import com.leasedemo.lease.application.repository.LeaseApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer boundary reacting to {@link LeaseApplicationApprovedEvent}
 * (M5.5 §19-21).
 *
 * <p>Flow: validate event -> reload the historical {@link LeaseApplication}
 * snapshot (never the current LeaseProduct configuration, M5.5 §21) ->
 * generate the confirmation PDF -> send the email -> return normally so
 * Spring Kafka commits the offset only after all of that succeeds
 * (M5.5 §34).
 *
 * <p>This class is intentionally the ONLY integration point between Kafka
 * and the PDF/email pipeline — it could later be extracted into its own
 * microservice without touching the {@link LeaseApplicationApprovedEvent}
 * contract (M5.5 §4).
 *
 * <p><strong>Known limitation (M5.5 §33):</strong> Kafka consumer delivery
 * is effectively at-least-once. In an extreme failure window (e.g. a crash
 * after sending the email but before the offset commit), the email could
 * theoretically be sent twice. No idempotency/processed-event table is
 * built in this speed milestone — a future evolution would add one.
 */
@Component
public class LeaseApplicationApprovedConsumer {

    private static final Logger log = LoggerFactory.getLogger(LeaseApplicationApprovedConsumer.class);

    private final LeaseApplicationRepository leaseApplicationRepository;
    private final LeaseApplicationPdfService pdfService;
    private final LeaseApplicationMailService mailService;

    public LeaseApplicationApprovedConsumer(
            LeaseApplicationRepository leaseApplicationRepository,
            LeaseApplicationPdfService pdfService,
            LeaseApplicationMailService mailService) {
        this.leaseApplicationRepository = leaseApplicationRepository;
        this.pdfService = pdfService;
        this.mailService = mailService;
    }

    @KafkaListener(
            topics = KafkaTopicNames.LEASE_APPLICATION_APPROVED,
            groupId = KafkaConsumerGroups.LEASE_PDF_MAIL)
    public void onApproved(LeaseApplicationApprovedEvent event) {
        if (event == null || event.applicationId() == null) {
            log.error("received an invalid LeaseApplicationApprovedEvent (null or missing applicationId)");
            return;
        }

        log.info("approved application event received: eventId={}, applicationId={}",
                event.eventId(), event.applicationId());

        // Load/close the DB interaction BEFORE generating the PDF/sending
        // email (M5.5 §23) — the historical snapshot, never the current
        // LeaseProduct configuration (M5.5 §21).
        LeaseApplication application = leaseApplicationRepository.findByIdWithCustomer(event.applicationId())
                .orElseThrow(() -> new LeaseApplicationConsumerException(
                        "No LeaseApplication found for applicationId=" + event.applicationId()));
        log.info("application snapshot loaded: applicationId={}", event.applicationId());

        try {
            byte[] pdf = pdfService.generate(application);
            log.info("PDF generated: applicationId={}, bytes={}", event.applicationId(), pdf.length);

            mailService.sendApprovedConfirmation(application, pdf);
            log.info("email sent: applicationId={}", event.applicationId());
        } catch (RuntimeException e) {
            log.error("failed to process approved application event: eventId={}, applicationId={}",
                    event.eventId(), event.applicationId(), e);
            throw e;
        }
    }
}
