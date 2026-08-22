package com.luxsoft.dxc.exchange.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Spring configuration for the NBP {@link RestClient} bean.
 *
 * <p>Configures the base URL pointing to the Polish National Bank public API.
 * The bean is injected into {@link com.luxsoft.dxc.exchange.client.NbpClient}.
 */
@Configuration
public class NbpClientConfig {

    private static final String NBP_BASE_URL = "https://api.nbp.pl";

    /**
     * Creates a {@link RestClient} pre-configured with the NBP base URL.
     *
     * <p>Named {@code nbpRestClient} to avoid conflicts with other RestClient beans
     * that may be registered in the application context.
     */
    @Bean
    public RestClient nbpRestClient() {
        return RestClient.builder()
                .baseUrl(NBP_BASE_URL)
                .build();
    }
}
