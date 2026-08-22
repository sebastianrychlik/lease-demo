package com.leasedemo.service;

import com.leasedemo.config.ApplicationProperties;
import com.leasedemo.dto.HealthResponse;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

    private final ApplicationProperties applicationProperties;

    public HealthService(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    public HealthResponse getHealth() {
        return new HealthResponse(
                "UP",
                "lease-demo",
                applicationProperties.getVersion()
        );
    }
}
