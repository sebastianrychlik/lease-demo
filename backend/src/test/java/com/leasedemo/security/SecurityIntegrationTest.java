package com.leasedemo.security;

import com.leasedemo.config.SecurityConfig;
import com.leasedemo.controller.HealthController;
import com.leasedemo.exchange.controller.ExchangeRateController;
import com.leasedemo.exchange.dto.response.ExchangeRateDto;
import com.leasedemo.exchange.dto.response.ExchangeRateResponse;
import com.leasedemo.exchange.service.ExchangeRateService;
import com.leasedemo.service.HealthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring Security integration tests for the LeaseDemo backend.
 *
 * <p>Uses {@link WebMvcTest} (web-layer slice) with the real {@link SecurityConfig} imported
 * to test the {@link com.leasedemo.config.SecurityConfig} authorization rules in isolation,
 * without requiring the full application context, a running Keycloak instance, or the NBP API.
 *
 * <p>JWT authentication is simulated via Spring Security's
 * {@link org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors#jwt()},
 * which injects a mock {@link org.springframework.security.oauth2.jwt.Jwt} authentication
 * token directly into the test security context — bypassing the real {@link JwtDecoder}.
 *
 * <p>{@link JwtDecoder} is also registered as a {@link MockBean} to prevent Spring Boot's
 * OAuth2 resource server auto-configuration from attempting to call the Keycloak OIDC
 * discovery endpoint during context setup.
 *
 * <p>Covered scenarios:
 * <ol>
 *   <li><strong>A</strong> — {@code GET /api/health}: no JWT → 200 OK (public endpoint)</li>
 *   <li><strong>B</strong> — {@code GET /api/exchange-rates}: no JWT → 401 Unauthorized</li>
 *   <li><strong>C</strong> — {@code GET /api/exchange-rates}: valid mock JWT → 200 OK</li>
 *   <li><strong>D</strong> — Role-based rule on {@code /actuator/**} (requires ADMIN):
 *     <ul>
 *       <li>ROLE_CUSTOMER → 403 Forbidden (access denied)</li>
 *       <li>No JWT → 401 Unauthorized</li>
 *       <li>ROLE_ADMIN → security passes (no handler in web-layer slice → 404)</li>
 *     </ul>
 *   </li>
 *   <li><strong>Default-deny</strong> — unknown path: ADMIN JWT → 403 Forbidden</li>
 * </ol>
 */
@WebMvcTest(controllers = {HealthController.class, ExchangeRateController.class})
@Import(SecurityConfig.class)
@ActiveProfiles("local")
@DisplayName("Security integration tests")
class SecurityIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    /**
     * Prevents Spring Boot's OAuth2 resource server auto-configuration from attempting
     * to call the Keycloak OIDC discovery endpoint during the test context setup.
     * JWT validation in tests is bypassed via {@code SecurityMockMvcRequestPostProcessors.jwt()}.
     */
    @MockBean
    JwtDecoder jwtDecoder;

    /** Prevents outbound NBP API calls from the real service. */
    @MockBean
    ExchangeRateService exchangeRateService;

    /** Required by HealthController in the web-layer slice. */
    @MockBean
    HealthService healthService;

    // ──────────────────────────────────────────────────────────────────────────
    // A. Public health endpoint — no authentication required
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("A — GET /api/health without JWT → 200 OK (public endpoint)")
    void healthEndpoint_noJwt_returns200() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // B. Protected endpoint — no authentication → 401
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("B — GET /api/exchange-rates without JWT → 401 Unauthorized")
    void exchangeRates_noJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/exchange-rates"))
                .andExpect(status().isUnauthorized());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // C. Protected endpoint — valid mock JWT → request accepted
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("C — GET /api/exchange-rates with CUSTOMER JWT → 200 OK")
    void exchangeRates_validCustomerJwt_returns200() throws Exception {
        when(exchangeRateService.getExchangeRates()).thenReturn(
                new ExchangeRateResponse(
                        "150/A/NBP/2025",
                        "2025-08-01",
                        List.of(new ExchangeRateDto("USD", "dolar amerykański", 4.05))));

        mockMvc.perform(get("/api/exchange-rates")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("C — GET /api/exchange-rates with ADVISOR JWT → 200 OK")
    void exchangeRates_validAdvisorJwt_returns200() throws Exception {
        when(exchangeRateService.getExchangeRates()).thenReturn(
                new ExchangeRateResponse("150/A/NBP/2025", "2025-08-01", List.of()));

        mockMvc.perform(get("/api/exchange-rates")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADVISOR"))))
                .andExpect(status().isOk());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // D. Role-based authorization — /actuator/** requires ROLE_ADMIN
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("D — GET /actuator/metrics with ROLE_CUSTOMER → 403 Forbidden (insufficient role)")
    void actuatorMetrics_customerRole_returns403() throws Exception {
        // Security rule: /actuator/** → hasRole("ADMIN"). CUSTOMER lacks this role → 403.
        mockMvc.perform(get("/actuator/metrics")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("D — GET /actuator/metrics with ROLE_ADMIN → security passes (no handler in slice → 404)")
    void actuatorMetrics_adminRole_passesSecurityCheck() throws Exception {
        // Security rule: /actuator/** → hasRole("ADMIN"). ADMIN passes → request reaches MVC layer.
        // Actuator endpoints are not loaded in @WebMvcTest web-layer slice → 404 (no handler).
        // This verifies that security ALLOWS the request rather than blocking it with 403/401.
        mockMvc.perform(get("/actuator/metrics")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("D — GET /actuator/metrics without JWT → 401 Unauthorized")
    void actuatorMetrics_noJwt_returns401() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Default-deny — unmatched paths
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("anyRequest().denyAll() — GET /unknown-path with ADMIN JWT → 403 Forbidden")
    void unknownPath_adminJwt_returns403() throws Exception {
        // The final security rule anyRequest().denyAll() prevents access to any path
        // not explicitly configured, even for authenticated ADMIN users.
        mockMvc.perform(get("/unknown-path")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isForbidden());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // E. Swagger UI / OpenAPI documentation — public (M4.1.1)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("E — GET /swagger-ui/index.html without JWT → not blocked by Spring Security")
    void swaggerUi_noJwt_notBlockedBySecurity() throws Exception {
        // springdoc resources are not registered in this @WebMvcTest web-layer slice
        // (no controllers for HealthController/ExchangeRateController), so the result
        // is 404 (no handler) rather than 200. The key assertion is that Spring
        // Security does NOT reject the request with 401/403 — /swagger-ui/** is
        // explicitly whitelisted before anyRequest().denyAll().
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("E — GET /v3/api-docs without JWT → not blocked by Spring Security")
    void apiDocs_noJwt_notBlockedBySecurity() throws Exception {
        // Same rationale as above: /v3/api-docs/** is permitAll in SecurityConfig,
        // so the request reaches (the absent) MVC handler in this slice → 404, not 401/403.
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isNotFound());
    }
}
