package com.leasedemo.exchange.dto.response;

import java.util.List;

/**
 * Internal DTO representing the full exchange rates API response.
 *
 * <p>Wraps the list of rates with metadata from the NBP table.
 * This is the public API contract exposed through our REST endpoint.
 */
public record ExchangeRateResponse(

        /** NBP table number, e.g. "150/A/NBP/2025". */
        String tableNo,

        /** Effective date of the rates, e.g. "2025-08-01". */
        String effectiveDate,

        /** List of individual currency exchange rates. */
        List<ExchangeRateDto> rates
) {}
