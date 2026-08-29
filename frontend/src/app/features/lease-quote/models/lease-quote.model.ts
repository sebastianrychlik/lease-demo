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
  currency: LeaseCurrency;
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
 */
export interface LeaseQuoteResponse {
  productCode: string;
  productName: string;

  vehiclePriceOriginal: number;
  currency: LeaseCurrency;
  exchangeRate: number;
  exchangeRateDate: string | null;
  vehiclePricePln: number;

  termMonths: number;

  initialPaymentPercent: number;
  initialPaymentPln: number;

  buyoutPercent: number;
  buyoutPln: number;

  leaseType: LeaseType;
  annualRatePercent: number;

  financedAmountPln: number;
  monthlyPaymentPln: number;
  totalLeaseCostPln: number;
  estimatedVatPln: number;
}
