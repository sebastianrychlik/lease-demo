package com.leasedemo.lease.application.pdf;

import com.leasedemo.lease.application.dto.InsuranceCoverageSnapshot;
import com.leasedemo.lease.application.entity.LeaseApplication;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Generates a small, in-memory Lease Application confirmation PDF (M5.5
 * §24-27).
 *
 * <p>Input is always the historical {@link LeaseApplication} snapshot
 * loaded by the Kafka consumer - no value here is recalculated; every
 * figure is read directly from the persisted row (M5.5 §26).
 *
 * <p>PDFBox's built-in standard Helvetica font does not reliably render
 * Polish diacritics, so - per M5.5 §27 - this demo keeps all PDF labels in
 * plain English rather than introducing a bundled font/typography
 * subsystem.
 *
 * <p>The PDF is never written to disk or Cloud Storage - it exists only as
 * an in-memory {@code byte[]} email attachment (M5.5 §24).
 */
@Service
public class LeaseApplicationPdfService {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'", Locale.ENGLISH).withZone(ZoneOffset.UTC);

    private static final float MARGIN = 50f;
    private static final float LEADING = 16f;

    public byte[] generate(LeaseApplication application) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDFont titleFont = PDType1Font.HELVETICA_BOLD;
            PDFont bodyFont = PDType1Font.HELVETICA;

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float y = page.getMediaBox().getHeight() - MARGIN;

                y = writeLine(content, titleFont, 18, MARGIN, y, "LeaseDemo - Lease Application Confirmation");
                y -= 6;
                y = writeLine(content, bodyFont, 11, MARGIN, y,
                        "Application ID: " + application.getId());
                y = writeLine(content, bodyFont, 11, MARGIN, y,
                        "Submitted: " + (application.getSubmittedAt() != null
                                ? DATE_FORMAT.format(application.getSubmittedAt()) : "-"));
                y = writeLine(content, bodyFont, 11, MARGIN, y,
                        "Decision: " + application.getStatus());
                y = writeLine(content, bodyFont, 11, MARGIN, y,
                        "Credit score: " + application.getCreditScore());

                y -= 10;
                y = writeLine(content, titleFont, 13, MARGIN, y, "Product");
                y = writeLine(content, bodyFont, 11, MARGIN, y,
                        "Product: " + application.getProductNameSnapshot() + " (" + application.getProductCode() + ")");
                y = writeLine(content, bodyFont, 11, MARGIN, y,
                        "Vehicle price: " + format(application.getVehiclePriceOriginal())
                                + " " + application.getVehiclePriceCurrency());
                y = writeLine(content, bodyFont, 11, MARGIN, y,
                        "Settlement currency: " + application.getSettlementCurrency());
                if (application.getExchangeRate() != null) {
                    y = writeLine(content, bodyFont, 11, MARGIN, y,
                            "Exchange rate: " + format(application.getExchangeRate())
                                    + (application.getExchangeRateDate() != null
                                            ? " (" + application.getExchangeRateDate() + ")" : ""));
                }

                y -= 10;
                y = writeLine(content, titleFont, 13, MARGIN, y, "Lease Terms");
                y = writeLine(content, bodyFont, 11, MARGIN, y,
                        "Lease term: " + application.getTermMonths() + " months");
                y = writeLine(content, bodyFont, 11, MARGIN, y,
                        "Initial payment: " + format(application.getInitialPayment())
                                + " (" + format(application.getInitialPaymentPercent()) + "%)");
                y = writeLine(content, bodyFont, 11, MARGIN, y,
                        "Buyout: " + format(application.getBuyout())
                                + " (" + format(application.getBuyoutPercent()) + "%)");
                y = writeLine(content, bodyFont, 11, MARGIN, y,
                        "Lease type: " + application.getLeaseType());
                y = writeLine(content, bodyFont, 11, MARGIN, y,
                        "Annual rate: " + format(application.getAnnualRatePercent()) + "%");
                y = writeLine(content, bodyFont, 11, MARGIN, y,
                        "Monthly lease payment: " + format(application.getMonthlyPayment())
                                + " " + application.getSettlementCurrency());

                y -= 10;
                y = writeLine(content, titleFont, 13, MARGIN, y, "Insurance");
                for (InsuranceCoverageSnapshot coverage : application.getInsuranceConfiguration()) {
                    y = writeLine(content, bodyFont, 11, MARGIN, y,
                            "- " + coverage.code()
                                    + (coverage.option() != null ? " (" + coverage.option() + ")" : "")
                                    + ": " + format(coverage.monthlyPremium()) + " " + application.getSettlementCurrency());
                }
                y = writeLine(content, bodyFont, 11, MARGIN, y,
                        "Insurance monthly premium: " + format(application.getInsuranceMonthlyPremium())
                                + " " + application.getSettlementCurrency());

                y -= 10;
                BigDecimal estimatedMonthlyTotal =
                        application.getMonthlyPayment().add(application.getInsuranceMonthlyPremium());
                writeLine(content, titleFont, 13, MARGIN, y,
                        "Estimated monthly total: " + format(estimatedMonthlyTotal)
                                + " " + application.getSettlementCurrency());
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate LeaseApplication PDF", e);
        }
    }

    private static String format(BigDecimal value) {
        return value == null ? "-" : value.toPlainString();
    }

    private static float writeLine(PDPageContentStream content, PDFont font, float fontSize,
                                    float x, float y, String text) throws IOException {
        content.beginText();
        content.setFont(font, fontSize);
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
        return y - LEADING;
    }
}
