package com.leasedemo.exchange.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Configuration for the Redis-backed {@code nbpExchangeRates} cache
 * (M5.1.6).
 *
 * <p>Bound from:
 * <pre>
 * leasedemo:
 *   cache:
 *     exchange-rates:
 *       ttl: PT6H
 * </pre>
 *
 * <p>NBP rates do not change every few seconds, while a 6-hour default TTL
 * removes repetitive external API calls during normal application use
 * without creating effectively permanent (stale-forever) cached data.
 */
@Component
@ConfigurationProperties(prefix = "leasedemo.cache.exchange-rates")
public class NbpCacheProperties {

    private Duration ttl = Duration.ofHours(6);

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }
}
