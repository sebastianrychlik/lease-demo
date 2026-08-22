package com.leasedemo.exchange.dto.response;

/**
 * Internal DTO representing a single exchange rate in our public API response.
 *
 * <p>This is the public API contract exposed through our REST endpoint.
 * It is deliberately separate from the NBP external DTOs.
 */
public record ExchangeRateDto(

        /** ISO 4217 currency code, e.g. "USD". */
        String code,

        /** Full currency name, e.g. "dolar amerykański". */
        String name,

        /** Mid exchange rate relative to Polish Złoty (PLN). */
        Double midRate
) {}
