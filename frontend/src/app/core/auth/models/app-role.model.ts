/**
 * Application-level role representation.
 *
 * LeaseDemo Angular code must never scatter raw Keycloak realm-role string
 * checks (e.g. `roles.includes('ADMIN')`) throughout components. This file
 * is the single, centralized home for interpreting Keycloak realm roles as
 * LeaseDemo application roles.
 *
 * Keycloak realm roles currently defined in
 * `infrastructure/keycloak/lease-demo-realm.json`: `ADMIN`, `CUSTOMER`,
 * `ADVISOR` (plus Keycloak's own `offline_access`/`uma_authorization`).
 *
 * `ADVISOR` has no corresponding application area in this milestone and is
 * intentionally NOT mapped to an {@link AppRole} — a user with only the
 * ADVISOR realm role is treated as having no recognized LeaseDemo
 * application role (fails closed, see {@link mapKeycloakRolesToAppRoles}).
 */
export enum AppRole {
  Admin = 'ADMIN',
  Customer = 'CUSTOMER',
}

/** Keycloak realm-role name that maps to {@link AppRole.Admin}. */
const KEYCLOAK_REALM_ROLE_ADMIN = 'ADMIN';

/** Keycloak realm-role name that maps to {@link AppRole.Customer}. */
const KEYCLOAK_REALM_ROLE_CUSTOMER = 'CUSTOMER';

/**
 * Maps raw Keycloak realm roles (as found in the access token's
 * `realm_access.roles`) to LeaseDemo {@link AppRole} values.
 *
 * Unrecognized Keycloak roles (e.g. `ADVISOR`, `offline_access`,
 * `uma_authorization`) are silently ignored here — they simply do not
 * contribute an {@link AppRole}. This is the ONLY place in the frontend
 * that should compare against these raw Keycloak role strings.
 */
export function mapKeycloakRolesToAppRoles(keycloakRoles: readonly string[]): AppRole[] {
  const appRoles: AppRole[] = [];

  if (keycloakRoles.includes(KEYCLOAK_REALM_ROLE_ADMIN)) {
    appRoles.push(AppRole.Admin);
  }

  if (keycloakRoles.includes(KEYCLOAK_REALM_ROLE_CUSTOMER)) {
    appRoles.push(AppRole.Customer);
  }

  return appRoles;
}
