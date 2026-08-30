package com.leasedemo.exchange.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.Map;

/**
 * Redis-backed Spring Cache configuration for the {@code nbpExchangeRates}
 * cache (M5.1.6).
 *
 * <p>Architectural rules honoured here:
 * <ul>
 *   <li>Redis is a CACHE, not the source of truth — NBP remains
 *       authoritative. See {@link #cacheErrorHandler()} for the fail-open
 *       behaviour: a Redis GET/PUT/EVICT/CLEAR failure is logged and
 *       swallowed rather than propagated, so a Redis outage never turns
 *       into a failed Lease Quote request by itself.</li>
 *   <li>TTL is per-cache-specific ({@code nbpExchangeRates} only), not a
 *       global default that would silently apply to future Redis caches.</li>
 *   <li>Null values are never cached.</li>
 *   <li>Keys are namespaced: {@code leasedemo:cache:<cacheName>::<key>},
 *       e.g. {@code leasedemo:cache:nbpExchangeRates::EUR}.</li>
 *   <li>Values are serialised as readable JSON (Jackson, with the
 *       {@code JavaTimeModule} registered so {@link java.time.LocalDate}
 *       serialises correctly) rather than Java native binary
 *       serialization.</li>
 * </ul>
 */
@Configuration
@EnableCaching
public class RedisCacheConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheConfig.class);

    private final NbpCacheProperties nbpCacheProperties;

    public RedisCacheConfig(NbpCacheProperties nbpCacheProperties) {
        this.nbpCacheProperties = nbpCacheProperties;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        RedisSerializationContext.SerializationPair<Object> valueSerializer =
                RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer(objectMapper));

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new StringRedisSerializer()))
                .serializeValuesWith(valueSerializer)
                .disableCachingNullValues()
                .computePrefixWith(cacheName -> "leasedemo:cache:" + cacheName + "::");

        // nbpExchangeRates is the ONLY cache with a single known concrete value
        // type (NbpRateSnapshot). GenericJackson2JsonRedisSerializer above
        // relies on an embedded "@class" JSON hint that is absent for values
        // written before this fix, and on a Redis HIT falls back to
        // deserializing into a raw LinkedHashMap, which Spring then fails to
        // cast to NbpRateSnapshot (ClassCastException / HTTP 500). Using a
        // typed Jackson2JsonRedisSerializer<NbpRateSnapshot> tells Jackson the
        // exact target type up front, so both SET and GET deserialize
        // correctly without any polymorphic/default-typing metadata.
        RedisSerializationContext.SerializationPair<NbpRateSnapshot> nbpRateSnapshotSerializer =
                RedisSerializationContext.SerializationPair.fromSerializer(
                        new Jackson2JsonRedisSerializer<>(objectMapper, NbpRateSnapshot.class));

        RedisCacheConfiguration nbpExchangeRatesConfig = defaultConfig
                .serializeValuesWith(nbpRateSnapshotSerializer)
                .entryTtl(nbpCacheProperties.getTtl());

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(Map.of(
                        NbpCacheNames.NBP_EXCHANGE_RATES, nbpExchangeRatesConfig))
                .build();
    }

    /**
     * Fail-open cache error handler (M5.1.6 §19/§20). Redis is an
     * optimization: any cache GET/PUT/EVICT/CLEAR failure (e.g. Redis is
     * down or unreachable) is logged at WARN and swallowed, so the
     * {@code @Cacheable}-annotated method still executes against NBP and
     * the Lease Quote request still succeeds. A genuine NBP failure is
     * unaffected by this handler and continues to propagate as
     * {@code NbpClientException} -> HTTP 502 via
     * {@code GlobalExceptionHandler}.
     */
    @Override
    @Bean
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.warn("Redis cache GET failed for cache={} key={}; falling back to source: {}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, org.springframework.cache.Cache cache, Object key, Object value) {
                log.warn("Redis cache PUT failed for cache={} key={}; result was not cached: {}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.warn("Redis cache EVICT failed for cache={} key={}: {}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, org.springframework.cache.Cache cache) {
                log.warn("Redis cache CLEAR failed for cache={}: {}", cache.getName(), exception.getMessage());
            }
        };
    }
}
