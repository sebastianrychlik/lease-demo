/**
 * Typed model representing a single currency exchange rate.
 *
 * Maps to the backend {@code ExchangeRateDto}.
 */
export interface ExchangeRate {
  /** ISO 4217 currency code, e.g. "USD". */
  code: string;

  /** Full currency name, e.g. "dolar amerykański". */
  name: string;

  /** Mid exchange rate relative to Polish Złoty (PLN). */
  midRate: number;
}

/**
 * Typed model representing the full exchange rates API response.
 *
 * Maps to the backend {@code ExchangeRateResponse}.
 */
export interface ExchangeRateResponse {
  /** NBP table number, e.g. "150/A/NBP/2025". */
  tableNo: string;

  /** Effective date of the rates, e.g. "2025-08-01". */
  effectiveDate: string;

  /** List of individual currency exchange rates. */
  rates: ExchangeRate[];
}
