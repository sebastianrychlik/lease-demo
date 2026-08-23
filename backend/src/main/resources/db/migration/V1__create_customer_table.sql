-- M4.1 — Customer domain.
--
-- keycloak_user_id is the Keycloak JWT `sub` claim identifying the owning
-- Keycloak account. Passwords and roles are never stored here — Keycloak
-- remains the sole source of truth for authentication/authorization.
-- Automatic JWT extraction into this column is not yet implemented.
--
-- The raw PESEL is never stored. Instead:
--   * pesel_encrypted — Base64(IV || AES-256-GCM ciphertext || auth tag), application-level encrypted.
--   * pesel_lookup    — deterministic lowercase-hex HMAC-SHA-256 digest, used for uniqueness
--                       and equality lookups without ever decrypting the ciphertext.
CREATE TABLE customers (
    id                UUID PRIMARY KEY,
    keycloak_user_id  VARCHAR(255) NOT NULL,
    first_name        VARCHAR(255) NOT NULL,
    last_name         VARCHAR(255) NOT NULL,
    email             VARCHAR(255) NOT NULL,
    phone_number      VARCHAR(30),
    date_of_birth     DATE NOT NULL,
    gender            VARCHAR(10) NOT NULL,
    pesel_encrypted   TEXT NOT NULL,
    pesel_lookup      VARCHAR(64) NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL,
    updated_at        TIMESTAMPTZ NOT NULL,

    CONSTRAINT uq_customers_keycloak_user_id UNIQUE (keycloak_user_id),
    CONSTRAINT uq_customers_pesel_lookup UNIQUE (pesel_lookup)
);
