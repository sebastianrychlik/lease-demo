/** Supported Admin Customer list sort fields — mirrors the backend allow-list. */
export type CustomerSortField = 'firstName' | 'lastName' | 'email' | 'dateOfBirth' | 'createdAt';

export type SortDirection = 'asc' | 'desc';

/**
 * Typed frontend query representation for GET /api/customers.
 *
 * Kept as a single typed object so HTTP parameter construction stays
 * centralized in {@link CustomerService} rather than scattered across
 * components.
 */
export interface CustomerQuery {
  page: number;
  size: number;
  search: string | null;
  sortField: CustomerSortField | null;
  sortDirection: SortDirection | null;
}

export const DEFAULT_CUSTOMER_QUERY: CustomerQuery = {
  page: 0,
  size: 20,
  search: null,
  sortField: null,
  sortDirection: null,
};
