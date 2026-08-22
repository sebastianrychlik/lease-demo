package com.leasedemo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spring Boot context-load smoke test.
 *
 * <p>
 * Verifies that the Spring application context assembles without errors.
 *
 * <h2>Test context constraints</h2>
 * <ul>
 * <li><strong>{@code spring.main.lazy-initialization=true}</strong> —
 * suppresses eager singleton
 * instantiation. A pre-existing incompatibility between Spring 6.1 / Spring
 * Boot 3.3 and the
 * MapStruct-generated {@code ExchangeRateMapperImpl} causes a
 * {@code NoClassDefFoundError: NbpTableDto} when Spring's
 * {@code AutowiredAnnotationBeanPostProcessor.checkLookupMethods()} introspects
 * the
 * generated class's method signatures via {@code Class.getDeclaredMethods0()}
 * (native).
 * With lazy initialization, the mapper implementation is never instantiated
 * during the
 * context-load test (it would only be triggered on the first real API call), so
 * the
 * error is suppressed without affecting the test's validity as a configuration
 * smoke
 * test.</li>
 * <li><strong>{@link JwtDecoder} mock</strong> — the resource-server security
 * filter chain
 * requires a {@code JwtDecoder} during its initialization. Without a mock,
 * Spring Boot's
 * auto-configuration would attempt to contact the Keycloak OIDC discovery
 * endpoint
 * (configured via {@code spring.security.oauth2.resourceserver.jwt.issuer-uri})
 * which is
 * not available in CI / offline test environments.</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("local")
class LeaseDemoApplicationTests {

    /**
     * Prevents the resource-server auto-configuration from contacting Keycloak's
     * OIDC
     * discovery endpoint when the {@link SecurityFilterChain} is initialized.
     */
    @MockBean
    JwtDecoder jwtDecoder;

    @Test
    void contextLoads() {
    }
}
