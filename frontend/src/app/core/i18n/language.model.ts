/**
 * Supported UI languages (M5.1.3).
 *
 * IMPORTANT: UI language is independent from LeaseMarket (PL/DE business
 * market). Do not couple this list to LeaseMarket values — `de` is
 * intentionally NOT a supported UI language in this milestone.
 */
export type AppLanguage = 'pl' | 'en';

export const AVAILABLE_LANGUAGES: readonly AppLanguage[] = ['pl', 'en'];

export const DEFAULT_LANGUAGE: AppLanguage = 'pl';

export const FALLBACK_LANGUAGE: AppLanguage = 'en';

/** localStorage key used to persist the user's selected UI language. */
export const LANGUAGE_STORAGE_KEY = 'leasedemo.language';
