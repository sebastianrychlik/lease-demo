/**
 * Insurance Configurator domain model (M5.2).
 *
 * Intentionally tiny — Insurance is NOT a configurable backend domain like
 * Lease Products. All coverage definitions below are frontend-only DEMO
 * constants; there is no insurance backend, database, or ADMIN CRUD in this
 * milestone (see docs/prompts M5.2 §5, §29, §30).
 */

/** The three demo coverages offered by the Insurance Configurator (M5.2 §5). */
export type InsuranceCoverageCode = 'GAP' | 'ASSISTANCE' | 'REPLACEMENT_CAR';

/** A single selectable option within a coverage (e.g. GAP variant, Assistance territory). */
export interface InsuranceCoverageOptionDefinition {
  value: string;
  labelKey: string;
  monthlyPremium: number;
}

/**
 * Static, frontend-only definition of one insurance coverage.
 *
 * `options` is present only for coverages with a dynamic sub-choice (GAP,
 * Assistance). `monthlyPremium` is the flat premium used when a coverage
 * has no options (Replacement Car).
 */
export interface InsuranceCoverageDefinition {
  code: InsuranceCoverageCode;
  nameTranslationKey: string;
  descriptionTranslationKey: string;
  monthlyPremium: number;
  options?: InsuranceCoverageOptionDefinition[];
}

/** The three demo coverage definitions — the sole source of dynamic form generation (M5.2 §6). */
export const INSURANCE_COVERAGE_DEFINITIONS: readonly InsuranceCoverageDefinition[] = [
  {
    code: 'GAP',
    nameTranslationKey: 'insurance.coverages.gap.name',
    descriptionTranslationKey: 'insurance.coverages.gap.description',
    monthlyPremium: 0,
    options: [
      { value: 'STANDARD', labelKey: 'insurance.options.standard', monthlyPremium: 79 },
      { value: 'PREMIUM', labelKey: 'insurance.options.premium', monthlyPremium: 109 },
    ],
  },
  {
    code: 'ASSISTANCE',
    nameTranslationKey: 'insurance.coverages.assistance.name',
    descriptionTranslationKey: 'insurance.coverages.assistance.description',
    monthlyPremium: 0,
    options: [
      { value: 'POLAND', labelKey: 'insurance.options.poland', monthlyPremium: 29 },
      { value: 'EUROPE', labelKey: 'insurance.options.europe', monthlyPremium: 49 },
    ],
  },
  {
    code: 'REPLACEMENT_CAR',
    nameTranslationKey: 'insurance.coverages.replacementCar.name',
    descriptionTranslationKey: 'insurance.coverages.replacementCar.description',
    monthlyPremium: 39,
  },
];

/** A single coverage row in the final configuration emitted to the parent (M5.2 §22). */
export interface InsuranceCoverageSelection {
  code: InsuranceCoverageCode;
  enabled: boolean;
  option: string | null;
  monthlyPremium: number;
}

/** The full insurance configuration emitted by `InsuranceConfiguratorComponent` to the parent page. */
export interface InsuranceConfiguration {
  coverages: InsuranceCoverageSelection[];
  totalMonthlyPremium: number;
}
