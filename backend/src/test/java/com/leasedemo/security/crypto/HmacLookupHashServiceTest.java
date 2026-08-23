package com.leasedemo.security.crypto;

import com.leasedemo.config.CryptoProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("HmacLookupHashService")
class HmacLookupHashServiceTest {

    private HmacLookupHashService service;

    @BeforeEach
    void setUp() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);

        CryptoProperties properties = new CryptoProperties();
        properties.setAesKey(Base64.getEncoder().encodeToString(key));
        properties.setHmacKey(Base64.getEncoder().encodeToString(key));

        service = new HmacLookupHashService(properties);
    }

    @Test
    @DisplayName("is deterministic for the same input")
    void hash_sameInputTwice_producesSameHash() {
        assertThat(service.hash("44051401359")).isEqualTo(service.hash("44051401359"));
    }

    @Test
    @DisplayName("produces different hashes for different inputs")
    void hash_differentInputs_producesDifferentHashes() {
        assertThat(service.hash("44051401359")).isNotEqualTo(service.hash("02270800027"));
    }

    @Test
    @DisplayName("produces a 64-character lowercase hex digest (SHA-256 output)")
    void hash_producesHexDigestOfExpectedLength() {
        String hash = service.hash("44051401359");
        assertThat(hash).hasSize(64).matches("[0-9a-f]+");
    }

    @Test
    @DisplayName("fails fast when the HMAC key is missing")
    void constructor_missingKey_throws() {
        CryptoProperties properties = new CryptoProperties();
        properties.setHmacKey(null);

        assertThatThrownBy(() -> new HmacLookupHashService(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("hmac-key");
    }

    @Test
    @DisplayName("fails fast when the HMAC key is not valid Base64")
    void constructor_invalidBase64_throws() {
        CryptoProperties properties = new CryptoProperties();
        properties.setHmacKey("not-valid-base64!!!");

        assertThatThrownBy(() -> new HmacLookupHashService(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Base64");
    }

    @Test
    @DisplayName("fails fast when the decoded HMAC key is shorter than 32 bytes")
    void constructor_keyTooShort_throws() {
        byte[] tooShort = new byte[16];
        new SecureRandom().nextBytes(tooShort);

        CryptoProperties properties = new CryptoProperties();
        properties.setHmacKey(Base64.getEncoder().encodeToString(tooShort));

        assertThatThrownBy(() -> new HmacLookupHashService(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }
}
