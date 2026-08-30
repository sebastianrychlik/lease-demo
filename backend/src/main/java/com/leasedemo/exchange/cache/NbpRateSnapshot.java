package com.leasedemo.exchange.cache;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Internal value object representing a cached canonical NBP foreign
 * currency -> PLN exchange-rate snapshot (M5.1.6).
 *
 * <p>This is the value stored in the {@code nbpExchangeRates} Redis cache
 * (via Spring Cache / {@link CachedNbpRateProvider}). It is deliberately a
 * small, clean internal type — never the raw NBP transport DTOs
 * ({@code NbpTableDto}/{@code NbpRateDto}) and never leaked into the public
 * Lease Quote API/DTOs.
 *
 * <p>{@code currency} is the foreign (non-PLN) currency code, e.g.
 * {@code "EUR"}. {@code rateToPln} is the NBP mid rate: the amount of PLN
 * for one unit of {@code currency}. {@code effectiveDate} is the NBP Table A
 * effective date associated with that rate, preserved verbatim across cache
 * hits so a Redis HIT produces the same Quote response semantics as an NBP
 * MISS (no {@code LocalDate.now()} fabrication).
 */
public record NbpRateSnapshot(
        String currency,
        BigDecimal rateToPln,
        LocalDate effectiveDate
) implements Serializable {
}
