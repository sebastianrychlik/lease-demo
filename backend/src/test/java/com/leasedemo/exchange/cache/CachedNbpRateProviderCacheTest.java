package com.leasedemo.exchange.cache;

import com.leasedemo.exchange.client.NbpClient;
import com.leasedemo.exchange.dto.external.NbpRateDto;
import com.leasedemo.exchange.dto.external.NbpTableDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the {@code @Cacheable} proxy behaviour of
 * {@link CachedNbpRateProvider} without requiring a real Redis instance
 * (M5.1.6 §46) — a simple in-memory {@link ConcurrentMapCacheManager} is
 * sufficient to prove that repeated lookups for the same currency reuse the
 * cached value and do not re-invoke {@link NbpClient}, guarding against a
 * self-invocation regression.
 */
@SpringBootTest(classes = {
        CachedNbpRateProvider.class,
        CachedNbpRateProviderCacheTest.TestCacheConfig.class
})
@DisplayName("CachedNbpRateProvider — Spring Cache proxy behaviour")
class CachedNbpRateProviderCacheTest {

    @Configuration
    @EnableCaching
    static class TestCacheConfig {
        @Bean
        public ConcurrentMapCacheManager cacheManager() {
            return new ConcurrentMapCacheManager(NbpCacheNames.NBP_EXCHANGE_RATES);
        }
    }

    @Autowired
    private CachedNbpRateProvider cachedNbpRateProvider;

    @MockBean
    private NbpClient nbpClient;

    @Test
    @DisplayName("second lookup for the same currency is served from cache — NbpClient called only once")
    void secondLookupIsCacheHit() {
        NbpTableDto table = new NbpTableDto("A", "167/A/NBP/2026", "2026-08-29",
                List.of(new NbpRateDto("euro", "EUR", 4.3328)));
        when(nbpClient.fetchTableA()).thenReturn(List.of(table));

        NbpRateSnapshot first = cachedNbpRateProvider.getRateToPln("EUR");
        NbpRateSnapshot second = cachedNbpRateProvider.getRateToPln("EUR");

        assertThat(first).isEqualTo(second);
        verify(nbpClient, times(1)).fetchTableA();
    }
}
