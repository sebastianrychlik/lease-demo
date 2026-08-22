package com.leasedemo.exchange.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * External DTO representing one exchange-rate table entry in the NBP response.
 *
 * <p>Maps directly to the JSON structure returned by:
 * {@code GET https://api.nbp.pl/api/exchangerates/tables/A?format=json}
 *
 * <p>This class is used exclusively for deserialisation of the NBP response.
 * It must never be exposed through our public REST API.
 */
public record NbpTableDto(

        @JsonProperty("table")
        String table,

        @JsonProperty("no")
        String no,

        @JsonProperty("effectiveDate")
        String effectiveDate,

        @JsonProperty("rates")
        List<NbpRateDto> rates
) {}
