import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Translation, TranslocoLoader } from '@jsverse/transloco';

/**
 * Loads static UI translation dictionaries from `/assets/i18n/{lang}.json`.
 *
 * Only STATIC UI text lives here (M5.1.3) — dynamic business content
 * (Lease Product names, future ADMIN-managed offer descriptions, etc.)
 * remains backend/PostgreSQL-driven and is never routed through Transloco.
 */
@Injectable({ providedIn: 'root' })
export class TranslocoHttpLoader implements TranslocoLoader {
  private readonly http = inject(HttpClient);

  getTranslation(lang: string) {
    return this.http.get<Translation>(`/assets/i18n/${lang}.json`);
  }
}
