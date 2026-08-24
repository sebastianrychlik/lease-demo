/**
 * Application-owned generic paged-response envelope.
 *
 * Maps to the backend {@code PageResponse<T>} contract shared by all
 * server-side paged LeaseDemo list endpoints.
 */
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}
