import { LeaseCurrency, LeaseType } from './lease-quote.model';
import { InsuranceCoverageCode } from './insurance.model';

/** Decision outcome returned by the backend (M5.3 §8). API values remain English. */
export type ApplicationStatus = 'APPROVED' | 'REVIEW' | 'REJECTED';

/** A single selected insurance coverage submitted to the backend (M5.3 §4). Disabled coverages are omitted. */
export interface InsuranceSelectionRequest {
  code: InsuranceCoverageCode;
  option: string | null;
}

/**
 * Outbound payload for `POST /api/lease-applications` (M5.3 §4).
 *
 * Carries QUOTE INPUTS only — never backend-authoritative result fields
 * such as monthlyPayment/exchangeRate/financedAmount/totalLeaseCost, and
 * never customerId/creditScore/status (M5.3 §31, §37, §38).
 */
export interface CreateLeaseApplicationRequest {
  productCode: string;
  vehiclePrice: number;
  vehiclePriceCurrency: LeaseCurrency;
  termMonths: number;
  initialPaymentPercent: number;
  buyoutPercent: number;
  leaseType: LeaseType;

  insurance: InsuranceSelectionRequest[];

  monthlyNetIncome: number;
  monthlyObligations: number;
}

/** Inbound payload from `POST /api/lease-applications` (M5.3 §17). */
export interface LeaseApplicationResponse {
  applicationId: string;
  status: ApplicationStatus;
  creditScore: number;
  submittedAt: string;

  productCode: string;
  productName: string;

  settlementCurrency: LeaseCurrency;

  monthlyPayment: number;
  insuranceMonthlyPremium: number;
  estimatedMonthlyTotal: number;
}
