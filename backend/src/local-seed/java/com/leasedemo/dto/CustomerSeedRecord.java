package com.leasedemo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.leasedemo.entity.Gender;

import java.time.LocalDate;

/**
 * A single LOCAL-DEVELOPMENT-ONLY synthetic Customer seed record, as
 * produced by {@code scripts/generate-customer-mock-data.py} (M4.1.3).
 *
 * <p><strong>Build-time isolation:</strong> this class lives under
 * {@code src/local-seed/java}, compiled/packaged only when the
 * {@code local-seed} Maven profile is explicitly activated — it is absent
 * from the normal production artifact (see {@code backend/pom.xml}).
 *
 * <p><strong>Trust boundary:</strong> {@code keycloakUserId} exists here
 * only as trusted local seed metadata read from a local, developer-owned
 * file. It is deliberately NOT part of {@link CustomerCreateRequest} — a
 * production HTTP client can never supply it. {@code CustomerSeedRunner}
 * passes it to {@code CustomerService.createCustomer(String,
 * CustomerCreateRequest)} exactly the way {@code CustomerController}
 * passes the authenticated JWT {@code sub}, as a separate, out-of-band
 * trusted identity argument.
 *
 * <p>{@code pesel} is plaintext here only transiently, in a local,
 * gitignored, disposable file — it is validated, encrypted, and hashed by
 * the real {@code CustomerService} pipeline immediately upon import, and is
 * never logged.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CustomerSeedRecord(
        String keycloakUserId,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        LocalDate dateOfBirth,
        Gender gender,
        String pesel
) {

    /**
     * Converts this seed record's business fields into the same
     * {@link CustomerCreateRequest} shape a real client would submit.
     * {@code keycloakUserId} is intentionally excluded — it must be passed
     * to {@code CustomerService} separately, never through this request.
     */
    public CustomerCreateRequest toCreateRequest() {
        return new CustomerCreateRequest(firstName, lastName, email, phoneNumber, dateOfBirth, gender, pesel);
    }
}
