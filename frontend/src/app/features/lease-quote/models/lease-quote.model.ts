/** Supported vehicle price currencies (M5.1). */
export type LeaseCurrency = 'PLN' | 'EUR';

/** Supported lease terms, in months (M5.1). */
export type LeaseTermMonths = 24 | 36 | 48 | 60;

/** Supported lease types (M5.1). */
export type LeaseType = 'OPERATING' | 'FINANCIAL';

/**
 * Outbound payload for `POST /api/lease-quotes/calculate`.
 *
 * Maps to the backend `LeaseQuoteRequest`. The backend is the sole
 * calculation authority — this shape carries only raw form inputs.
 */
export interface LeaseQuoteRequest {
  vehiclePrice: number;
  currency: LeaseCurrency;
  termMonths: LeaseTermMonths;
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
  vehiclePriceOriginal: number;
  currency: LeaseCurrency;
  exchangeRate: number;
  exchangeRateDate: string | null;
  vehiclePricePln: number;

  termMonths: LeaseTermMonths;

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
