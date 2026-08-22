package com.leasedemo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HealthResponse(
        @JsonProperty("status") String status,
        @JsonProperty("application") String application,
        @JsonProperty("version") String version
) {
}
