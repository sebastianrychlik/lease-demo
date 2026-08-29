package com.leasedemo.dto;

import com.leasedemo.entity.Gender;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Outbound customer representation.
 *
 * <p>Deliberately excludes the raw PESEL, {@code peselEncrypted}, and
 * {@code peselLookup} — the PESEL (raw or derived, in any form) is never
 * returned by the API.
 */
public record CustomerResponse(
        UUID id,
        String keycloakUserId,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        LocalDate dateOfBirth,
        Gender gender,
        Instant createdAt,
        Instant updatedAt
) {
}
