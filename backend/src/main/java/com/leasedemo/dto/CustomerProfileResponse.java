package com.leasedemo.dto;

import com.leasedemo.entity.Gender;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Outbound self-service profile representation for {@code GET
 * /api/customers/me} (M4.5).
 *
 * <p>Deliberately excludes the raw PESEL, {@code peselEncrypted}, and
 * {@code peselLookup} — PESEL reveal is explicitly deferred to a future
 * milestone. Also excludes {@code keycloakUserId}: the browser already
 * knows its own JWT subject and does not need it echoed back, and this
 * DTO is not used for identity resolution (identity resolution happens
 * server-side, from the authenticated JWT).
 */
public record CustomerProfileResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        LocalDate dateOfBirth,
        Gender gender,
        Instant createdAt
) {
}
