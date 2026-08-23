package com.leasedemo.security.crypto;

import com.leasedemo.config.CryptoProperties;
import com.leasedemo.exception.EncryptionException;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Application-level field encryption using AES-256-GCM.
 *
 * <p>Used to encrypt sensitive business fields (e.g. PESEL) independently
 * of storage-level (disk/TLS) encryption, so that a database-level
 * compromise alone does not expose plaintext PESEL values.
 *
 * <p>Each call to {@link #encrypt(String)} generates a fresh random 96-bit
 * IV (the recommended nonce size for GCM) and prepends it to the ciphertext
 * before Base64-encoding the result, so no IV needs to be stored separately.
 */
@Component
public class AesGcmEncryptionService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKeySpec secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    /** Required AES-256 key length in bytes (256 bits). */
    private static final int AES_256_KEY_LENGTH_BYTES = 32;

    public AesGcmEncryptionService(CryptoProperties cryptoProperties) {
        String configuredKey = cryptoProperties.getAesKey();
        if (configuredKey == null || configuredKey.isBlank()) {
            throw new IllegalStateException(
                    "application.crypto.aes-key is not configured; set the CRYPTO_AES_KEY environment "
                            + "variable to a Base64-encoded 256-bit (32-byte) AES key");
        }

        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(configuredKey);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "application.crypto.aes-key (CRYPTO_AES_KEY) is not valid Base64", ex);
        }

        if (keyBytes.length != AES_256_KEY_LENGTH_BYTES) {
            throw new IllegalStateException(
                    "application.crypto.aes-key (CRYPTO_AES_KEY) must decode to exactly "
                            + AES_256_KEY_LENGTH_BYTES + " bytes (256 bits) for AES-256-GCM, but was "
                            + keyBytes.length + " bytes");
        }

        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Encrypts {@code plaintext}, returning a Base64 string of {@code IV || ciphertext || authTag}.
     */
    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (GeneralSecurityException ex) {
            throw new EncryptionException("Failed to encrypt field", ex);
        }
    }

    /**
     * Decrypts a value previously produced by {@link #encrypt(String)}.
     */
    public String decrypt(String encoded) {
        try {
            byte[] combined = Base64.getDecoder().decode(encoded);
            ByteBuffer buffer = ByteBuffer.wrap(combined);

            byte[] iv = new byte[IV_LENGTH_BYTES];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException ex) {
            throw new EncryptionException("Failed to decrypt field", ex);
        }
    }
}
