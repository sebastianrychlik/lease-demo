package com.leasedemo.exchange.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Serialization round-trip test for the {@code nbpExchangeRates} Redis value
 * serializer (M5.1.6 targeted fix).
 *
 * <p>Reproduces the exact serializer configuration used by
 * {@link RedisCacheConfig#cacheManager} for the {@code nbpExchangeRates}
 * cache — a typed {@code Jackson2JsonRedisSerializer<NbpRateSnapshot>} — and
 * exercises a real serialize/deserialize cycle (no Redis connection
 * required). This guards against the regression where
 * {@code GenericJackson2JsonRedisSerializer} reconstructed a Redis HIT as a
 * raw {@link java.util.LinkedHashMap} instead of {@link NbpRateSnapshot},
 * which the {@code ConcurrentMapCacheManager}-based
 * {@link CachedNbpRateProviderCacheTest} cannot catch because it bypasses
 * JSON serialization entirely.
 */
@DisplayName("nbpExchangeRates Redis value serializer — round-trip")
class RedisCacheConfigSerializationTest {

    @Test
    @DisplayName("serialize/deserialize NbpRateSnapshot preserves type, currency, rateToPln and effectiveDate")
    void roundTripPreservesTypeAndFields() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Jackson2JsonRedisSerializer<NbpRateSnapshot> serializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, NbpRateSnapshot.class);

        NbpRateSnapshot original = new NbpRateSnapshot(
                "EUR", new BigDecimal("4.3328"), LocalDate.of(2026, 8, 29));

        byte[] json = serializer.serialize(original);
        Object deserialized = serializer.deserialize(json);

        assertThat(deserialized)
                .isInstanceOf(NbpRateSnapshot.class)
                .isNotInstanceOf(java.util.LinkedHashMap.class);

        NbpRateSnapshot snapshot = (NbpRateSnapshot) deserialized;
        assertThat(snapshot.currency()).isEqualTo("EUR");
        assertThat(snapshot.rateToPln()).isEqualByComparingTo(new BigDecimal("4.3328"));
        assertThat(snapshot.effectiveDate()).isEqualTo(LocalDate.of(2026, 8, 29));
    }
}
