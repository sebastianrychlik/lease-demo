package com.leasedemo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Application-level field encryption key material.
 *
 * <p>Both keys are Base64-encoded and must be supplied via environment
 * variables (never committed to source control) — see
 * {@code application-local.yml} for the property names.
 *
 * <ul>
 *   <li>{@code aesKey} — a 256-bit AES key used for AES-256-GCM encryption
 *       of the raw PESEL value ({@code pesel_encrypted}).</li>
 *   <li>{@code hmacKey} — an HMAC-SHA-256 key used to derive a deterministic,
 *       non-reversible lookup hash of the PESEL ({@code pesel_lookup}), which
 *       allows equality lookups without ever decrypting stored data.</li>
 * </ul>
 */
@Component
@ConfigurationProperties(prefix = "application.crypto")
public class CryptoProperties {

    private String aesKey;
    private String hmacKey;

    public String getAesKey() {
        return aesKey;
    }

    public void setAesKey(String aesKey) {
        this.aesKey = aesKey;
    }

    public String getHmacKey() {
        return hmacKey;
    }

    public void setHmacKey(String hmacKey) {
        this.hmacKey = hmacKey;
    }
}
