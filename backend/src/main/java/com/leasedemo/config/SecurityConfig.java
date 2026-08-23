package com.leasedemo.config;

import com.leasedemo.security.KeycloakRealmRoleConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security configuration for the LeaseDemo backend acting as an
 * OAuth2 Resource Server.
 *
 * <p><strong>Architecture:</strong>
 * <ul>
 *   <li>Spring Boot does NOT handle login or password entry — Keycloak is the
 *       Identity Provider / Authorization Server.</li>
 *   <li>Angular forwards Keycloak-issued access tokens as {@code Bearer} JWTs
 *       in the {@code Authorization} header.</li>
 *   <li>Spring Security validates JWT signatures and standard claims using
 *       Keycloak's JWK endpoint (discovered via issuer-uri at startup).</li>
 *   <li>Realm roles from {@code realm_access.roles} are mapped to Spring
 *       Security authorities by {@link KeycloakRealmRoleConverter}.</li>
 * </ul>
 *
 * <p><strong>Endpoint policy:</strong>
 * <pre>
 *   /swagger-ui/**      →  public   (OpenAPI/Swagger UI documentation, M4.1.1)
 *   /v3/api-docs/**     →  public   (OpenAPI JSON, M4.1.1)
 *   /actuator/health    →  public   (deployment infrastructure health checks)
 *   /actuator/info      →  public
 *   /actuator/**        →  ROLE_ADMIN  (metrics and management require admin access)
 *   /api/health         →  public   (application health check)
 *   /api/**             →  authenticated  (any valid LeaseDemo JWT)
 *   everything else     →  deny
 * </pre>
 *
 * <p><strong>Session / CSRF / HTTP Basic / form login:</strong>
 * <ul>
 *   <li>Session policy: STATELESS — no server-side session is created.</li>
 *   <li>CSRF: disabled — not applicable for stateless Bearer JWT authentication.</li>
 *   <li>HTTP Basic: not configured.</li>
 *   <li>Form login: not configured.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Comma-separated list of allowed CORS origins.
     * <p>Local:      {@code http://localhost:4200}
     * <p>Production: injected via the {@code CORS_ALLOWED_ORIGINS} environment variable.
     */
    @Value("${application.cors.allowed-origins}")
    private String allowedOrigins;

    /**
     * Main security filter chain.
     *
     * <p>Configures:
     * <ul>
     *   <li>CORS using {@link #corsConfigurationSource()}</li>
     *   <li>CSRF disabled (stateless resource server)</li>
     *   <li>STATELESS session management</li>
     *   <li>Endpoint authorization rules</li>
     *   <li>OAuth2 JWT resource server with Keycloak realm-role converter</li>
     * </ul>
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ── CORS ──────────────────────────────────────────────────────────────
                // Delegate to the CorsConfigurationSource bean; handles OPTIONS preflight.
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ── CSRF ──────────────────────────────────────────────────────────────
                // Disabled: stateless REST API authenticated via Bearer JWT.
                // No session cookies, no CSRF attack surface.
                .csrf(AbstractHttpConfigurer::disable)

                // ── SESSION ───────────────────────────────────────────────────────────
                // STATELESS: Spring Security must not create or use HTTP sessions.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ── ENDPOINT AUTHORIZATION ────────────────────────────────────────────
                .authorizeHttpRequests(authz -> authz

                        // Preflight CORS requests must always pass through.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Swagger UI / OpenAPI documentation: public.
                        // NOTE (M4.1.1): API documentation exposure is a demo/interview
                        // convenience. In a real financial production deployment, whether
                        // to expose interactive API docs publicly is an explicit
                        // deployment/security decision (often restricted to internal
                        // networks or disabled entirely). This does NOT make /api/**
                        // public — protected endpoints remain governed by the rules below.
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // Actuator health/info: public — required by deployment infra.
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                        // All other actuator endpoints: restricted to ADMIN.
                        // Natural rule: metrics/env/beans are internal management data.
                        .requestMatchers("/actuator/**").hasRole("ADMIN")

                        // Application health endpoint: public — no auth required.
                        .requestMatchers(HttpMethod.GET, "/api/health").permitAll()

                        // All application API endpoints require a valid authenticated JWT.
                        .requestMatchers("/api/**").authenticated()

                        // Deny everything not explicitly matched above.
                        .anyRequest().denyAll()
                )

                // ── OAUTH2 RESOURCE SERVER ────────────────────────────────────────────
                // JWT decoder is auto-configured from spring.security.oauth2.resourceserver
                // .jwt.issuer-uri — Spring Security fetches the JWK set from Keycloak's
                // OIDC discovery endpoint at startup and validates incoming tokens locally.
                // No per-request calls to Keycloak are made during normal operation.
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        return http.build();
    }

    /**
     * Configures the JWT authentication converter to use {@link KeycloakRealmRoleConverter}.
     *
     * <p>Replaces the default scope-based authority extraction with Keycloak realm-role
     * extraction, mapping CUSTOMER / ADVISOR / ADMIN to the corresponding ROLE_ authorities.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        return converter;
    }

    /**
     * CORS configuration source.
     *
     * <p>Allows the Angular frontend to make cross-origin requests to the Spring Boot backend.
     * The allowed origin is injected from {@code application.cors.allowed-origins}:
     * <ul>
     *   <li>Local:      {@code http://localhost:4200}</li>
     *   <li>Production: from the {@code CORS_ALLOWED_ORIGINS} environment variable</li>
     * </ul>
     *
     * <p>Multiple origins can be provided as a comma-separated value.
     * Wildcard origins are not used — origins are explicitly enumerated.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Support comma-separated origins for production multi-domain scenarios.
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        configuration.setAllowedOrigins(origins);

        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(
                List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        // Credentials: allows Angular to send the Authorization header cross-origin.
        configuration.setAllowCredentials(true);
        // Cache preflight for 1 hour to reduce OPTIONS requests.
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
