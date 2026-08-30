package com.leasedemo.exchange.cache;

import com.leasedemo.exchange.client.NbpClient;
import com.leasedemo.exchange.dto.external.NbpRateDto;
import com.leasedemo.exchange.dto.external.NbpTableDto;
import com.leasedemo.exchange.exception.NbpClientException;
import com.leasedemo.lease.quote.exception.UnsupportedCurrencyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Caches the canonical NBP foreign-currency -> PLN rate (M5.1.6).
 *
 * <p>Deliberately a separate Spring bean from
 * {@link com.leasedemo.lease.quote.service.LeaseQuoteExchangeRateResolver}
 * so that the {@link Cacheable} annotation below is intercepted by the
 * Spring AOP proxy. If the caching method lived on the resolver itself and
 * were invoked from another method on that same bean (self-invocation), the
 * proxy would be bypassed and caching silently would not happen.
 *
 * <p>Only the foreign-currency -&gt; PLN direction is ever fetched/cached
 * (e.g. {@code EUR}). The inverse (PLN -&gt; foreign) is always derived
 * mathematically by the caller from this same canonical snapshot — never
 * fetched or cached separately (M5.1.6 §10/§24).
 *
 * <p>Redis is a cache only. If the underlying Redis-backed
 * {@code CacheManager} is unavailable, the configured
 * {@code CacheErrorHandler} (see {@code RedisCacheConfig}) swallows the
 * cache GET/PUT failure and this method still executes normally against
 * NBP — Redis outages never make Lease Quote unavailable by themselves.
 */
@Component
public class CachedNbpRateProvider {

    private static final Logger log = LoggerFactory.getLogger(CachedNbpRateProvider.class);

    private final NbpClient nbpClient;

    public CachedNbpRateProvider(NbpClient nbpClient) {
        this.nbpClient = nbpClient;
    }

    /**
     * Returns the canonical NBP {@code foreignCurrency -> PLN} snapshot
     * (rate + effective date), from Redis when present or from the NBP API
     * on a cache miss.
     *
     * <p>Cache key: the foreign currency code (e.g. {@code "EUR"}).
     * {@code "PLN"} must never be passed in — no NBP lookup is required for
     * PLN, and callers (the resolver) must short-circuit before reaching
     * this method.
     *
     * @param foreignCurrency ISO currency code of the non-PLN currency, e.g. {@code "EUR"}
     * @throws NbpClientException if the NBP API is unreachable or returns an unexpected response
     * @throws UnsupportedCurrencyException if NBP Table A does not publish a rate for {@code foreignCurrency}
     */
    @Cacheable(cacheNames = NbpCacheNames.NBP_EXCHANGE_RATES, key = "#foreignCurrency", unless = "#result == null")
    public NbpRateSnapshot getRateToPln(String foreignCurrency) {
        log.info("NBP exchange-rate cache miss for {} — fetching Table A", foreignCurrency);

        List<NbpTableDto> tables = nbpClient.fetchTableA();
        NbpTableDto firstTable = tables.stream()
                .findFirst()
                .orElseThrow(() -> new NbpClientException(
                        "NBP API response contained no exchange rate tables."));

        NbpRateDto rateDto = firstTable.rates().stream()
                .filter(rate -> foreignCurrency.equals(rate.code()))
                .findFirst()
                .orElseThrow(() -> new UnsupportedCurrencyException(
                        "No NBP exchange rate available for currency " + foreignCurrency));

        BigDecimal rateToPln = BigDecimal.valueOf(rateDto.mid());
        LocalDate effectiveDate = LocalDate.parse(firstTable.effectiveDate());

        return new NbpRateSnapshot(foreignCurrency, rateToPln, effectiveDate);
    }
}
