package com.leasedemo.exchange.cache;

/**
 * Central constant(s) for Spring Cache names used by the NBP exchange-rate
 * cache (M5.1.6), so the name is not scattered across multiple classes
 * ({@link CachedNbpRateProvider}, {@code RedisCacheConfig}).
 */
public final class NbpCacheNames {

    /**
     * Cache holding canonical NBP foreign-currency -> PLN rate snapshots
     * ({@link NbpRateSnapshot}), keyed by foreign currency code (e.g.
     * {@code "EUR"}). Backed by Redis via a dedicated
     * {@code RedisCacheConfiguration} (TTL, null-caching disabled).
     */
    public static final String NBP_EXCHANGE_RATES = "nbpExchangeRates";

    private NbpCacheNames() {
    }
}
