import { Injectable, Signal, computed, inject } from '@angular/core';
import { TranslocoService } from '@jsverse/transloco';

import {
  AppLanguage,
  AVAILABLE_LANGUAGES,
  DEFAULT_LANGUAGE,
  LANGUAGE_LOCALE,
  LANGUAGE_STORAGE_KEY,
} from './language.model';

/**
 * Resolves and persists the application's UI language (M5.1.3).
 *
 * Resolution order on startup: saved `localStorage` preference, otherwise
 * the default language (PL). Deliberately NOT bound to browser locale or
 * to LeaseMarket — see {@link AppLanguage}.
 */
@Injectable({ providedIn: 'root' })
export class LanguageService {
  private readonly translocoService = inject(TranslocoService);

  /**
   * Reactive locale for Angular's native number/currency/percent/date
   * formatters, DERIVED from the active Transloco language (M5.1.3.1).
   *
   * Transloco's `activeLang` is already a signal, so this locale updates
   * immediately (no reload) whenever `setLanguage`/`init` changes the active
   * language — there is no independent locale state to drift out of sync.
   */
  readonly locale: Signal<string> = computed(
    () => LANGUAGE_LOCALE[this.translocoService.activeLang() as AppLanguage],
  );

  /** Reads the persisted language (if any/valid) and activates it in Transloco. */
  init(): void {
    this.translocoService.setActiveLang(this.resolveInitialLanguage());
  }

  /** Switches the active UI language and persists the choice. */
  setLanguage(language: AppLanguage): void {
    this.translocoService.setActiveLang(language);
    localStorage.setItem(LANGUAGE_STORAGE_KEY, language);
  }

  /** The currently active UI language. */
  get currentLanguage(): AppLanguage {
    return this.translocoService.getActiveLang() as AppLanguage;
  }

  private resolveInitialLanguage(): AppLanguage {
    const saved = localStorage.getItem(LANGUAGE_STORAGE_KEY);
    if (saved && (AVAILABLE_LANGUAGES as readonly string[]).includes(saved)) {
      return saved as AppLanguage;
    }
    return DEFAULT_LANGUAGE;
  }
}
