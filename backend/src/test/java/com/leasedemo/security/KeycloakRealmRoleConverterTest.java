package com.leasedemo.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link KeycloakRealmRoleConverter}.
 *
 * <p>Verifies that:
 * <ul>
 *   <li>LeaseDemo roles (CUSTOMER, ADVISOR, ADMIN) are correctly mapped to ROLE_ authorities.</li>
 *   <li>Keycloak internal roles are excluded from the business authority set.</li>
 *   <li>Missing or malformed claims are handled safely (no exception, empty result).</li>
 * </ul>
 *
 * <p>No Spring context is required — these are plain unit tests.
 */
@DisplayName("KeycloakRealmRoleConverter")
class KeycloakRealmRoleConverterTest {

    private KeycloakRealmRoleConverter converter;

    @BeforeEach
    void setUp() {
        converter = new KeycloakRealmRoleConverter();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Individual LeaseDemo roles
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CUSTOMER role → ROLE_CUSTOMER")
    void convert_customerRole_mapsToRoleCustomer() {
        Jwt jwt = buildJwt(Map.of("realm_access", Map.of("roles", List.of("CUSTOMER"))));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorityNames(authorities)).containsExactly("ROLE_CUSTOMER");
    }

    @Test
    @DisplayName("ADVISOR role → ROLE_ADVISOR")
    void convert_advisorRole_mapsToRoleAdvisor() {
        Jwt jwt = buildJwt(Map.of("realm_access", Map.of("roles", List.of("ADVISOR"))));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorityNames(authorities)).containsExactly("ROLE_ADVISOR");
    }

    @Test
    @DisplayName("ADMIN role → ROLE_ADMIN")
    void convert_adminRole_mapsToRoleAdmin() {
        Jwt jwt = buildJwt(Map.of("realm_access", Map.of("roles", List.of("ADMIN"))));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorityNames(authorities)).containsExactly("ROLE_ADMIN");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Multiple roles
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Multiple LeaseDemo roles → all ROLE_ authorities")
    void convert_allThreeLeasesDemoRoles_mapsAllAuthorities() {
        Jwt jwt = buildJwt(Map.of(
                "realm_access", Map.of("roles", List.of("CUSTOMER", "ADVISOR", "ADMIN"))));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorityNames(authorities))
                .containsExactlyInAnyOrder("ROLE_CUSTOMER", "ROLE_ADVISOR", "ROLE_ADMIN");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Missing / empty claims
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Missing realm_access claim → empty authorities")
    void convert_missingRealmAccess_returnsEmptyList() {
        Jwt jwt = buildJwt(Map.of());

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).isEmpty();
    }

    @Test
    @DisplayName("realm_access present but roles key missing → empty authorities")
    void convert_missingRolesKey_returnsEmptyList() {
        Jwt jwt = buildJwt(Map.of("realm_access", Map.of()));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).isEmpty();
    }

    @Test
    @DisplayName("roles is an empty list → empty authorities")
    void convert_emptyRolesList_returnsEmptyList() {
        Jwt jwt = buildJwt(Map.of("realm_access", Map.of("roles", List.of())));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).isEmpty();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Keycloak internal roles are excluded
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Keycloak internal roles only → no LeaseDemo business authorities")
    void convert_keycloakInternalRolesOnly_returnsEmptyList() {
        Jwt jwt = buildJwt(Map.of("realm_access", Map.of(
                "roles", List.of("default-roles-lease-demo", "offline_access", "uma_authorization"))));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).isEmpty();
    }

    @Test
    @DisplayName("Mixed LeaseDemo + Keycloak internal roles → only LeaseDemo roles exposed")
    void convert_mixedRoles_onlyLeasesDemoRolesExposed() {
        Jwt jwt = buildJwt(Map.of("realm_access", Map.of(
                "roles", List.of(
                        "CUSTOMER",
                        "default-roles-lease-demo",
                        "offline_access",
                        "uma_authorization"))));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorityNames(authorities)).containsExactly("ROLE_CUSTOMER");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Malformed claim structures — safe handling
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("roles claim is a String, not a List → empty authorities (no exception)")
    void convert_rolesClaimIsString_returnsEmptyListSafely() {
        Jwt jwt = buildJwt(Map.of("realm_access", Map.of("roles", "CUSTOMER")));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).isEmpty();
    }

    @Test
    @DisplayName("realm_access is not a Map (malformed) → handled safely")
    void convert_realmAccessIsNotMap_returnsEmptyListSafely() {
        // realm_access as a plain string — malformed token structure.
        // Jwt.getClaimAsMap() returns null when the claim is not a Map,
        // so the converter should return an empty list without throwing.
        Jwt jwt = buildJwt(Map.of("realm_access", "malformed"));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).isEmpty();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Builds a minimal {@link Jwt} with the given extra claims for testing purposes.
     */
    private Jwt buildJwt(Map<String, Object> extraClaims) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .subject("test-user-id")
                .claims(claims -> claims.putAll(extraClaims))
                .build();
    }

    private Set<String> authorityNames(Collection<GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }
}
