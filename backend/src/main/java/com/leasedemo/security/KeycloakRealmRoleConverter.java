package com.leasedemo.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Converts Keycloak JWT realm roles into Spring Security {@link GrantedAuthority} instances.
 *
 * <p>Reads the {@code realm_access.roles} claim from the Keycloak access token and maps
 * only the LeaseDemo business roles (CUSTOMER, ADVISOR, ADMIN) to Spring Security
 * {@code ROLE_*} authorities. Keycloak internal / default roles such as
 * {@code default-roles-lease-demo}, {@code offline_access} and {@code uma_authorization}
 * are deliberately excluded from the LeaseDemo authorization model.
 *
 * <p>Authority mapping:
 * <pre>
 *   CUSTOMER  →  ROLE_CUSTOMER
 *   ADVISOR   →  ROLE_ADVISOR
 *   ADMIN     →  ROLE_ADMIN
 * </pre>
 *
 * <p>This converter is used together with Spring Security's {@link
 * org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter}
 * so that {@code hasRole("CUSTOMER")} in security rules corresponds to {@code ROLE_CUSTOMER}.
 */
public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    /**
     * LeaseDemo business roles that should become Spring Security authorities.
     * Any Keycloak role not present in this set is silently ignored.
     */
    private static final Set<String> LEASE_DEMO_ROLES = Set.of("CUSTOMER", "ADVISOR", "ADMIN");

    /**
     * Extracts LeaseDemo realm roles from the JWT {@code realm_access.roles} claim
     * and converts them into {@link GrantedAuthority} instances with the {@code ROLE_} prefix.
     *
     * @param jwt the validated Keycloak access token
     * @return collection of Spring Security authorities; never {@code null}
     */
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        // Use getClaim() (returns raw Object) rather than getClaimAsMap() which throws
        // IllegalArgumentException when the claim exists but is not a Map type.
        // The instanceof pattern-match safely handles null, String, or any non-Map value.
        Object realmAccessClaim = jwt.getClaim("realm_access");
        if (!(realmAccessClaim instanceof Map<?, ?> realmAccess)) {
            return Collections.emptyList();
        }

        Object rolesObject = realmAccess.get("roles");
        if (!(rolesObject instanceof List<?>)) {
            return Collections.emptyList();
        }

        return ((List<?>) rolesObject).stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(LEASE_DEMO_ROLES::contains)
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toUnmodifiableList());
    }
}
