package com.leasedemo.dto;

import com.leasedemo.entity.Gender;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Outbound row representation for the Admin Customer list (M4.4).
 *
 * <p>Deliberately excludes {@code peselEncrypted}, {@code peselLookup}, any
 * PESEL value (raw or derived), and {@code keycloakUserId} — none of these
 * are required by the Customer list table, and sensitive/internal fields
 * must not cross this API boundary without a concrete business
 * requirement.
 */
public record CustomerListItemResponse(
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
