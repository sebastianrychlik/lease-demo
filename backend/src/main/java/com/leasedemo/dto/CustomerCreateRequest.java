package com.leasedemo.dto;

import com.leasedemo.entity.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

/**
 * Inbound payload for customer registration.
 *
 * <p>{@code pesel} is validated structurally (11 digits) here, and
 * cross-checked against {@code dateOfBirth}/{@code gender} in
 * {@code PeselValidator}; it is never persisted in plaintext, logged, or
 * echoed back in any response/exception message.
 *
 * <p>{@code keycloakUserId} corresponds to the Keycloak JWT {@code sub}
 * claim. Automatic extraction from the authenticated JWT is not implemented
 * yet, so it must currently be supplied explicitly by the caller.
 */
public record CustomerCreateRequest(
        @NotBlank String keycloakUserId,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Email String email,
        String phoneNumber,
        @NotNull @Past LocalDate dateOfBirth,
        @NotNull Gender gender,
        @NotBlank @Pattern(regexp = "\\d{11}", message = "PESEL must be exactly 11 digits") String pesel
) {
}
