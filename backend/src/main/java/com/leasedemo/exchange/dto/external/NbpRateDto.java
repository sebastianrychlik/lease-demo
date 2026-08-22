package com.leasedemo.exchange.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * External DTO representing a single currency rate entry in the NBP Table A response.
 *
 * <p>Maps directly to the JSON structure returned by:
 * {@code GET https://api.nbp.pl/api/exchangerates/tables/A?format=json}
 *
 * <p>This class is used exclusively for deserialisation of the NBP response.
 * It must never be exposed through our public REST API.
 */
public record NbpRateDto(

        @JsonProperty("currency")
        String currency,

        @JsonProperty("code")
        String code,

        @JsonProperty("mid")
        Double mid
) {}
