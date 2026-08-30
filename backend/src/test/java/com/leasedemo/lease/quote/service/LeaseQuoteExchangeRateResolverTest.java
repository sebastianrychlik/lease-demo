package com.leasedemo.lease.quote.service;

import com.leasedemo.exchange.cache.CachedNbpRateProvider;
import com.leasedemo.exchange.cache.NbpRateSnapshot;
import com.leasedemo.lease.quote.model.LeaseCurrency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaseQuoteExchangeRateResolver")
class LeaseQuoteExchangeRateResolverTest {

    @Mock
    private CachedNbpRateProvider cachedNbpRateProvider;

    private LeaseQuoteExchangeRateResolver resolver;

    @Test
    @DisplayName("same currency short-circuits to rate=1 without calling CachedNbpRateProvider")
    void sameCurrencyShortCircuits() {
        resolver = new LeaseQuoteExchangeRateResolver(cachedNbpRateProvider);

        LeaseQuoteExchangeRateResolver.ResolvedRate eurToEur =
                resolver.resolve(LeaseCurrency.EUR, LeaseCurrency.EUR);
        LeaseQuoteExchangeRateResolver.ResolvedRate plnToPln =
                resolver.resolve(LeaseCurrency.PLN, LeaseCurrency.PLN);

        assertThat(eurToEur.rate()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(eurToEur.effectiveDate()).isNull();
        assertThat(plnToPln.rate()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(plnToPln.effectiveDate()).isNull();

        verifyNoInteractions(cachedNbpRateProvider);
    }

    @Test
    @DisplayName("EUR -> PLN returns the cached rate directly and preserves effectiveDate")
    void directRate() {
        when(cachedNbpRateProvider.getRateToPln("EUR"))
                .thenReturn(new NbpRateSnapshot("EUR", new BigDecimal("4.3328"), LocalDate.of(2026, 8, 29)));
        resolver = new LeaseQuoteExchangeRateResolver(cachedNbpRateProvider);

        LeaseQuoteExchangeRateResolver.ResolvedRate resolved =
                resolver.resolve(LeaseCurrency.EUR, LeaseCurrency.PLN);

        assertThat(resolved.rate()).isEqualByComparingTo(new BigDecimal("4.3328"));
        assertThat(resolved.effectiveDate()).isEqualTo("2026-08-29");
        verify(cachedNbpRateProvider).getRateToPln("EUR");
    }

    @Test
    @DisplayName("PLN -> EUR derives the inverse rate from the same canonical cached snapshot")
    void inverseRate() {
        when(cachedNbpRateProvider.getRateToPln("EUR"))
                .thenReturn(new NbpRateSnapshot("EUR", new BigDecimal("4.3328"), LocalDate.of(2026, 8, 29)));
        resolver = new LeaseQuoteExchangeRateResolver(cachedNbpRateProvider);

        LeaseQuoteExchangeRateResolver.ResolvedRate resolved =
                resolver.resolve(LeaseCurrency.PLN, LeaseCurrency.EUR);

        BigDecimal expected = BigDecimal.ONE.divide(new BigDecimal("4.3328"), 4, java.math.RoundingMode.HALF_UP);
        assertThat(resolved.rate()).isEqualByComparingTo(expected);
        assertThat(resolved.effectiveDate()).isEqualTo("2026-08-29");
        // Only one call to the canonical foreign-currency -> PLN provider —
        // no second/separate lookup for the inverse direction.
        verify(cachedNbpRateProvider).getRateToPln("EUR");
    }
}
