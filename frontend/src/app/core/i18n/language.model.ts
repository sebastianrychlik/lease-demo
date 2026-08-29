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

/**
 * Single source of truth mapping the UI language (Transloco) to the Angular
 * formatting locale used by number/currency/percent/date pipes (M5.1.3.1).
 *
 * This is the ONLY place `'pl-PL'` / `'en-US'` literals should exist —
 * components/templates must derive the locale from {@link AppLanguage} via
 * this map (typically through `LanguageService.locale`), never hard-code it.
 */
export const LANGUAGE_LOCALE: Record<AppLanguage, string> = {
  pl: 'pl-PL',
  en: 'en-US',
};
