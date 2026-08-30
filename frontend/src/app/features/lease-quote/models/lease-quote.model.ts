/** Supported vehicle price currencies — domain contract (M5.1.2). */
export type LeaseCurrency = 'PLN' | 'EUR';

/** Supported lease types — domain contract (M5.1.2). */
export type LeaseType = 'OPERATING' | 'FINANCIAL';

/**
 * Backend-owned min/max/default/step percentage range (M5.1.2).
 *
 * Rendered by Angular sliders — never hard-coded on the frontend.
 */
export interface PercentageRangeConfiguration {
  minPercent: number;
  maxPercent: number;
  defaultPercent: number;
  stepPercent: number;
}

/** A lease type offered by a product, with the APR actually used for calculation. */
export interface LeaseTypeOption {
  type: LeaseType;
  annualRatePercent: number;
}

/**
 * Backend-driven Lease Product configuration (M5.1.2).
 *
 * Maps to `LeaseProductConfigurationResponse`. Angular renders its Reactive
 * Form entirely from this shape (currencies, terms, ranges, lease types,
 * defaults) — it never hard-codes business options.
 */
export interface LeaseProductConfiguration {
  code: string;
  name: string;
  market: string;

  currencies: LeaseCurrency[];
  /**
   * The currency in which this product's lease amounts are calculated and
   * settled (M5.1.3.2). Distinct from `currencies` (accepted VEHICLE PRICE
   * currencies). Backend-owned — never derived on the frontend.
   */
  settlementCurrency: LeaseCurrency;
  termsMonths: number[];

  initialPayment: PercentageRangeConfiguration;
  buyout: PercentageRangeConfiguration;

  leaseTypes: LeaseTypeOption[];

  defaultCurrency: LeaseCurrency;
  defaultTermMonths: number;
  defaultLeaseType: LeaseType;
}

/**
 * Outbound payload for `POST /api/lease-quotes/calculate`.
 *
 * Maps to the backend `LeaseQuoteRequest`. The backend is the sole
 * calculation authority — this shape carries only raw form inputs, plus
 * the selected `productCode` (M5.1.2), which is now part of the quote
 * contract.
 */
export interface LeaseQuoteRequest {
  productCode: string;
  vehiclePrice: number;
  vehiclePriceCurrency: LeaseCurrency;
  termMonths: number;
  initialPaymentPercent: number;
  buyoutPercent: number;
  leaseType: LeaseType;
}

/**
 * Inbound payload from `POST /api/lease-quotes/calculate`.
 *
 * Maps to the backend `LeaseQuoteResponse`. Angular renders these values
 * as-is and never re-derives or duplicates the calculation.
 *
 * Currency-neutral (M5.1.3.2): `settlementCurrency` is resolved entirely by
 * the backend from the selected Lease Product — the frontend never sends
 * or derives it — and every monetary amount below is expressed in that
 * settlement currency.
 */
export interface LeaseQuoteResponse {
  productCode: string;
  productName: string;

  vehiclePriceOriginal: number;
  vehiclePriceCurrency: LeaseCurrency;

  settlementCurrency: LeaseCurrency;

  /** Amount of settlementCurrency for ONE unit of vehiclePriceCurrency. `1` when currencies match. */
  exchangeRate: number;
  exchangeRateDate: string | null;
  vehiclePriceSettlement: number;

  termMonths: number;

  initialPaymentPercent: number;
  initialPayment: number;

  buyoutPercent: number;
  buyout: number;

  leaseType: LeaseType;
  annualRatePercent: number;

  financedAmount: number;
  monthlyPayment: number;
  totalLeaseCost: number;
  estimatedVat: number;
}
