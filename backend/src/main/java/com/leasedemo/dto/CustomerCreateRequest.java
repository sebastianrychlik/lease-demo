package com.leasedemo.dto;

import com.leasedemo.entity.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
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
 * <p>This is untrusted client-supplied business data. It deliberately does
 * NOT contain the Keycloak identity ({@code keycloakUserId} / JWT
 * {@code sub}) — that value is derived exclusively from the authenticated
 * JWT by {@code CustomerController} and passed to {@code CustomerService}
 * out of band, so a client can never choose or override its own identity.
 */
public record CustomerCreateRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Email String email,
        String phoneNumber,
        @NotNull @Past LocalDate dateOfBirth,
        @NotNull Gender gender,
        @NotBlank @Pattern(regexp = "\\d{11}", message = "PESEL must be exactly 11 digits")
        @Schema(description = "Polish PESEL national identification number. "
                + "Accepted only in the request body; never persisted in plaintext.")
        String pesel
) {
}
