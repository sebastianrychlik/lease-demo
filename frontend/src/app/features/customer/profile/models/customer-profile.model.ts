/**
 * Self-service Customer profile model.
 *
 * Maps to the backend {@code CustomerProfileResponse} (GET
 * /api/customers/me). Deliberately excludes PESEL (raw/encrypted/lookup)
 * and keycloakUserId — the profile never needs to know its own Keycloak
 * subject, and PESEL reveal is explicitly deferred to a future milestone.
 */
export interface CustomerProfile {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string | null;
  dateOfBirth: string;
  gender: 'MALE' | 'FEMALE';
  createdAt: string;
}

/**
 * Outbound onboarding request payload (POST /api/customers).
 *
 * The Keycloak identity (keycloakUserId / JWT sub) is deliberately absent
 * — the backend derives it exclusively from the authenticated JWT. This
 * type must never gain a customerId/keycloakUserId field.
 */
export interface CustomerCreateRequest {
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
  dateOfBirth: string;
  gender: 'MALE' | 'FEMALE';
  pesel: string;
}
