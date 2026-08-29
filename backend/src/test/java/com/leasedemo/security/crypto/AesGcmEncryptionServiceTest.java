package com.leasedemo.security.crypto;

import com.leasedemo.config.CryptoProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AesGcmEncryptionService")
class AesGcmEncryptionServiceTest {

    private AesGcmEncryptionService service;

    @BeforeEach
    void setUp() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);

        CryptoProperties properties = new CryptoProperties();
        properties.setAesKey(Base64.getEncoder().encodeToString(key));
        properties.setHmacKey(Base64.getEncoder().encodeToString(key));

        service = new AesGcmEncryptionService(properties);
    }

    @Test
    @DisplayName("decrypts what it encrypted")
    void encryptThenDecrypt_roundTrips() {
        String plaintext = "44051401359";

        String ciphertext = service.encrypt(plaintext);
        assertThat(ciphertext).isNotEqualTo(plaintext);

        String decrypted = service.decrypt(ciphertext);
        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("produces different ciphertext for the same plaintext on each call (random IV)")
    void encrypt_sameInputTwice_producesDifferentCiphertext() {
        String plaintext = "44051401359";

        String first = service.encrypt(plaintext);
        String second = service.encrypt(plaintext);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("fails fast when the AES key is missing")
    void constructor_missingKey_throws() {
        CryptoProperties properties = new CryptoProperties();
        properties.setAesKey(null);

        assertThatThrownBy(() -> new AesGcmEncryptionService(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("aes-key");
    }

    @Test
    @DisplayName("fails fast when the AES key is not valid Base64")
    void constructor_invalidBase64_throws() {
        CryptoProperties properties = new CryptoProperties();
        properties.setAesKey("not-valid-base64!!!");

        assertThatThrownBy(() -> new AesGcmEncryptionService(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Base64");
    }

    @Test
    @DisplayName("fails fast when the decoded AES key is not exactly 32 bytes")
    void constructor_wrongKeyLength_throws() {
        byte[] tooShort = new byte[16];
        new SecureRandom().nextBytes(tooShort);

        CryptoProperties properties = new CryptoProperties();
        properties.setAesKey(Base64.getEncoder().encodeToString(tooShort));

        assertThatThrownBy(() -> new AesGcmEncryptionService(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }
}
