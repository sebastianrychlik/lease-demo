package com.leasedemo.security.crypto;

import com.leasedemo.config.CryptoProperties;
import com.leasedemo.exception.EncryptionException;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Derives a deterministic, non-reversible HMAC-SHA-256 lookup hash for a
 * sensitive field (e.g. PESEL).
 *
 * <p>AES-256-GCM ciphertext is non-deterministic (a fresh random IV is used
 * per encryption), so it cannot be used directly for equality lookups
 * (e.g. "does a customer with this PESEL already exist?"). This service
 * instead computes a deterministic keyed hash — {@code pesel_lookup} — that
 * can be indexed and compared without ever decrypting stored data or
 * exposing the raw PESEL.
 */
@Component
public class HmacLookupHashService {

    private static final String ALGORITHM = "HmacSHA256";

    private final SecretKeySpec secretKey;

    /**
     * Minimum enforced HMAC key strength for this project: 32 bytes (256
     * bits), matching the AES key size for symmetry and clarity.
     */
    private static final int MIN_HMAC_KEY_LENGTH_BYTES = 32;

    public HmacLookupHashService(CryptoProperties cryptoProperties) {
        String configuredKey = cryptoProperties.getHmacKey();
        if (configuredKey == null || configuredKey.isBlank()) {
            throw new IllegalStateException(
                    "application.crypto.hmac-key is not configured; set the CRYPTO_HMAC_KEY environment "
                            + "variable to a Base64-encoded key of at least "
                            + MIN_HMAC_KEY_LENGTH_BYTES + " bytes (256 bits)");
        }

        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(configuredKey);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "application.crypto.hmac-key (CRYPTO_HMAC_KEY) is not valid Base64", ex);
        }

        if (keyBytes.length < MIN_HMAC_KEY_LENGTH_BYTES) {
            throw new IllegalStateException(
                    "application.crypto.hmac-key (CRYPTO_HMAC_KEY) must decode to at least "
                            + MIN_HMAC_KEY_LENGTH_BYTES + " bytes (256 bits), but was "
                            + keyBytes.length + " bytes");
        }

        this.secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
    }

    /**
     * Computes the lowercase hex-encoded HMAC-SHA-256 of {@code value}.
     */
    public String hash(String value) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(secretKey);
            byte[] digest = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException ex) {
            throw new EncryptionException("Failed to compute lookup hash", ex);
        }
    }
}
