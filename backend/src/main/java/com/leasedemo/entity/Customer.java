package com.leasedemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A LeaseDemo customer.
 *
 * <p>{@link #keycloakUserId} is the Keycloak JWT {@code sub} claim
 * identifying the owning Keycloak account. No password or role information
 * is stored here — Keycloak remains the sole source of truth for
 * authentication/authorization. This value is derived exclusively from the
 * validated, authenticated JWT (see {@code CustomerController}) — it is
 * never accepted as client-supplied request data.
 *
 * <p>The raw PESEL value is never persisted. Instead:
 *
 * <ul>
 *   <li>{@link #peselEncrypted} stores Base64(IV || AES-256-GCM ciphertext ||
 *       auth tag) of the PESEL (see {@code AesGcmEncryptionService}), used to
 *       recover the plaintext value when strictly necessary.</li>
 *   <li>{@link #peselLookup} stores a deterministic lowercase-hex
 *       HMAC-SHA-256 hash of the PESEL (see {@code HmacLookupHashService}),
 *       used to enforce uniqueness and support equality lookups without
 *       decryption.</li>
 * </ul>
 *
 * <p>{@link #createdAt} and {@link #updatedAt} are owned exclusively by the
 * backend and set automatically via {@link PrePersist}/{@link PreUpdate}.
 */
@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "keycloak_user_id", nullable = false, unique = true)
    private String keycloakUserId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 10)
    private Gender gender;

    @Column(name = "pesel_encrypted", nullable = false, columnDefinition = "text")
    private String peselEncrypted;

    @Column(name = "pesel_lookup", nullable = false, unique = true, length = 64)
    private String peselLookup;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Customer() {
        // required by JPA
    }

    public Customer(
            String keycloakUserId,
            String firstName,
            String lastName,
            String email,
            String phoneNumber,
            LocalDate dateOfBirth,
            Gender gender,
            String peselEncrypted,
            String peselLookup) {
        this.keycloakUserId = keycloakUserId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.peselEncrypted = peselEncrypted;
        this.peselLookup = peselLookup;
    }

    /**
     * Sets both timestamps on first insert — the backend is the sole owner
     * of {@code created_at}/{@code updated_at}, never the client.
     */
    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Refreshes {@code updated_at} on every subsequent modification.
     */
    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getKeycloakUserId() {
        return keycloakUserId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public Gender getGender() {
        return gender;
    }

    public String getPeselEncrypted() {
        return peselEncrypted;
    }

    public String getPeselLookup() {
        return peselLookup;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
