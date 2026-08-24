/**
 * Admin Customer list row model.
 *
 * Maps to the backend {@code CustomerListItemResponse} (GET /api/customers).
 * Deliberately excludes PESEL (raw/encrypted/lookup) and keycloakUserId —
 * the Admin Customer list does not need them, and sensitive fields never
 * cross this boundary without a concrete business requirement.
 */
export interface CustomerListItem {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string | null;
  dateOfBirth: string;
  gender: 'MALE' | 'FEMALE';
  createdAt: string;
}
