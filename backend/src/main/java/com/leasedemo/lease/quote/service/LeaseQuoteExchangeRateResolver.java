package com.leasedemo.lease.quote.service;

import com.leasedemo.exchange.cache.CachedNbpRateProvider;
import com.leasedemo.exchange.cache.NbpRateSnapshot;
import com.leasedemo.lease.quote.exception.UnsupportedCurrencyException;
import com.leasedemo.lease.quote.model.LeaseCurrency;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Resolves the exchange rate between a vehicle price currency and a
 * product's settlement currency (M5.1.3.2), via the Redis-backed
 * {@link CachedNbpRateProvider} (M5.1.6), which in turn reuses the existing
 * {@code NbpClient} NBP Table A integration on a cache miss.
 *
 * <p>Semantics: {@code exchangeRate} is the amount of settlement currency
 * for ONE unit of vehicle price (source) currency. For example, if the
 * NBP EUR/PLN mid rate is {@code 4.3328} then:
 * <ul>
 *   <li>EUR (source) -> PLN (settlement): {@code exchangeRate = 4.3328}
 *       — the NBP rate is used directly.</li>
 *   <li>PLN (source) -> EUR (settlement): {@code exchangeRate = 1 / 4.3328}
 *       — the mathematically correct inverse of the NBP rate, derived from
 *       the same canonical cached EUR -> PLN snapshot (no separate cache
 *       entry/NBP call for the inverse direction).</li>
 *   <li>same currency (source == settlement): {@code exchangeRate = 1},
 *       and neither Redis nor NBP is ever consulted (M5.1.3.2 §11
 *       same-currency short-circuit).</li>
 * </ul>
 *
 * <p>Only PLN/EUR are supported (M5.1 currency set). This class contains
 * no lease-calculation business logic and no Redis-specific types
 * ({@code RedisTemplate}/{@code RedisCacheManager} etc.) — only
 * currency-to-rate resolution against the semantic
 * {@link CachedNbpRateProvider} bean.
 */
@Component
public class LeaseQuoteExchangeRateResolver {

    /** NBP mid rates are published with 4 decimal places. */
    private static final int NBP_RATE_SCALE = 4;
    private static final MathContext INVERSION_MATH_CONTEXT = new MathContext(20, RoundingMode.HALF_UP);

    private final CachedNbpRateProvider cachedNbpRateProvider;

    public LeaseQuoteExchangeRateResolver(CachedNbpRateProvider cachedNbpRateProvider) {
        this.cachedNbpRateProvider = cachedNbpRateProvider;
    }

    /**
     * A resolved exchange rate together with the NBP table's effective date
     * (or {@code null} when no NBP lookup was performed, i.e. same-currency).
     */
    public record ResolvedRate(BigDecimal rate, String effectiveDate) {
    }

    /**
     * Resolves the exchange rate to convert an amount in
     * {@code sourceCurrency} (vehicle price currency) into
     * {@code settlementCurrency}.
     */
    public ResolvedRate resolve(LeaseCurrency sourceCurrency, LeaseCurrency settlementCurrency) {
        if (sourceCurrency == settlementCurrency) {
            return new ResolvedRate(BigDecimal.ONE.setScale(NBP_RATE_SCALE, RoundingMode.HALF_UP), null);
        }

        // M5.1 supports only PLN/EUR — NBP Table A publishes the EUR/PLN mid
        // rate directly. Any other (source, settlement) currency pair is not
        // currently reachable given LeaseCurrency's enum values, but guard
        // against it explicitly rather than silently misinterpreting the rate.
        if (sourceCurrency != LeaseCurrency.PLN && settlementCurrency != LeaseCurrency.PLN) {
            throw new UnsupportedCurrencyException(
                    "No supported NBP conversion path between " + sourceCurrency + " and " + settlementCurrency);
        }

        LeaseCurrency nonPlnCurrency = sourceCurrency == LeaseCurrency.PLN ? settlementCurrency : sourceCurrency;

        NbpRateSnapshot snapshot = cachedNbpRateProvider.getRateToPln(nonPlnCurrency.name());
        BigDecimal nonPlnToPlnRate = snapshot.rateToPln().setScale(NBP_RATE_SCALE, RoundingMode.HALF_UP);
        String effectiveDate = snapshot.effectiveDate().toString();

        if (settlementCurrency == LeaseCurrency.PLN) {
            // sourceCurrency (nonPlnCurrency) -> PLN: use the NBP rate directly.
            return new ResolvedRate(nonPlnToPlnRate, effectiveDate);
        }

        // sourceCurrency is PLN, settlementCurrency is nonPlnCurrency: use the
        // mathematically correct inverse of the NBP rate. Computed via
        // BigDecimal (never double), then re-scaled for consistent display.
        BigDecimal inverseRate = BigDecimal.ONE
                .divide(nonPlnToPlnRate, INVERSION_MATH_CONTEXT)
                .setScale(NBP_RATE_SCALE, RoundingMode.HALF_UP);
        return new ResolvedRate(inverseRate, effectiveDate);
    }
}
