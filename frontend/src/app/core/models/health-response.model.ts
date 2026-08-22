/**
 * Typed model representing the backend health endpoint response.
 *
 * Contract: GET /api/health
 *
 * Example response:
 * {
 *   "status": "UP",
 *   "application": "lease-demo",
 *   "version": "0.1.0"
 * }
 */
export interface HealthResponse {
  /** Backend reported status, e.g. "UP". */
  status: string;
  /** Application name reported by the backend. */
  application: string;
  /** Application version reported by the backend. */
  version: string;
}
