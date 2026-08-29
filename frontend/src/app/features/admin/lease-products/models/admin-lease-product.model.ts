import { LeaseCurrency, LeaseType, PercentageRangeConfiguration } from '../../../lease-quote/models/lease-quote.model';

export type { LeaseCurrency, LeaseType, PercentageRangeConfiguration };

/** A lease type configured on a product, with its APR (M5.1.4). */
export interface LeaseTypeOption {
  type: LeaseType;
  annualRatePercent: number;
}

/**
 * ADMIN-facing Lease Product configuration (M5.1.4).
 *
 * Maps to the backend `AdminLeaseProductResponse`. Distinct from the
 * CUSTOMER-facing `LeaseProductConfiguration` (reused from the Lease Quote
 * feature) — this shape additionally carries `enabled`, validity dates, and
 * audit timestamps needed for ADMIN management.
 */
export interface AdminLeaseProduct {
  code: string;
  name: string;
  market: string;
  enabled: boolean;

  validFrom: string | null;
  validTo: string | null;

  currencies: LeaseCurrency[];
  settlementCurrency: LeaseCurrency;
  defaultCurrency: LeaseCurrency;

  termsMonths: number[];
  defaultTermMonths: number;

  initialPayment: PercentageRangeConfiguration;
  buyout: PercentageRangeConfiguration;

  leaseTypes: LeaseTypeOption[];
  defaultLeaseType: LeaseType;

  createdAt: string;
  updatedAt: string;
}

/** Outbound percentage-range shape shared by create/update requests. */
export interface PercentageRangeRequest {
  minPercent: number;
  maxPercent: number;
  defaultPercent: number;
  stepPercent: number;
}

/** Outbound lease-type option shape shared by create/update requests. */
export interface LeaseTypeOptionRequest {
  type: LeaseType;
  annualRatePercent: number;
}

/** Payload for `POST /api/admin/lease-products`. */
export interface CreateLeaseProductRequest {
  code: string;
  name: string;
  market: string;
  enabled: boolean;

  validFrom: string | null;
  validTo: string | null;

  currencies: LeaseCurrency[];
  settlementCurrency: LeaseCurrency;
  defaultCurrency: LeaseCurrency;

  terms: number[];
  defaultTermMonths: number;

  initialPayment: PercentageRangeRequest;
  buyout: PercentageRangeRequest;

  leaseTypes: LeaseTypeOptionRequest[];
  defaultLeaseType: LeaseType;
}

/**
 * Payload for `PUT /api/admin/lease-products/{code}`.
 *
 * Deliberately has no `code` — product code is stable identity and is
 * never changed by update (M5.1.4 §10-11).
 */
export type UpdateLeaseProductRequest = Omit<CreateLeaseProductRequest, 'code'>;
