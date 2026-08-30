package com.leasedemo.lease.application.mail;

import com.leasedemo.lease.application.entity.LeaseApplication;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

/**
 * Sends the LeaseDemo APPROVED confirmation email with the generated PDF
 * attached, via the existing local Mailpit SMTP container (M5.5 §28-31).
 *
 * <p>Recipient resolution prefers the real {@code Customer.email}; a
 * configurable demo fallback is used only if it is ever missing/blank
 * (M5.5 §22) — no larger Customer-profile milestone is introduced here.
 */
@Service
public class LeaseApplicationMailService {

    private final JavaMailSender mailSender;
    private final String demoRecipient;

    public LeaseApplicationMailService(
            JavaMailSender mailSender,
            @Value("${leasedemo.mail.demo-recipient}") String demoRecipient) {
        this.mailSender = mailSender;
        this.demoRecipient = demoRecipient;
    }

    public void sendApprovedConfirmation(LeaseApplication application, byte[] pdf) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            helper.setTo(resolveRecipient(application));
            helper.setSubject("LeaseDemo - Application APPROVED - " + application.getId());
            helper.setText(bodyFor(application), false);
            helper.addAttachment("lease-application-" + application.getId() + ".pdf",
                    new org.springframework.core.io.ByteArrayResource(pdf));

            mailSender.send(message);
        } catch (jakarta.mail.MessagingException | MailException e) {
            throw new LeaseApplicationMailException(
                    "Failed to send LeaseDemo approval email for application " + application.getId(), e);
        }
    }

    private String resolveRecipient(LeaseApplication application) {
        String email = application.getCustomer() != null ? application.getCustomer().getEmail() : null;
        return StringUtils.hasText(email) ? email : demoRecipient;
    }

    private String bodyFor(LeaseApplication application) {
        BigDecimal estimatedMonthlyTotal =
                application.getMonthlyPayment().add(application.getInsuranceMonthlyPremium());

        return "Your LeaseDemo application has been approved." + System.lineSeparator() + System.lineSeparator()
                + "Application ID: " + application.getId() + System.lineSeparator()
                + "Credit score: " + application.getCreditScore() + System.lineSeparator()
                + "Monthly lease payment: " + application.getMonthlyPayment() + " " + application.getSettlementCurrency() + System.lineSeparator()
                + "Insurance: " + application.getInsuranceMonthlyPremium() + " " + application.getSettlementCurrency() + System.lineSeparator()
                + "Estimated monthly total: " + estimatedMonthlyTotal + " " + application.getSettlementCurrency() + System.lineSeparator()
                + System.lineSeparator()
                + "Your application confirmation PDF is attached.";
    }
}
