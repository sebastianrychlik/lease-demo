import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';

import { LanguageService } from '../../../core/i18n/language.service';
import { AppLanguage, AVAILABLE_LANGUAGES } from '../../../core/i18n/language.model';

/**
 * Compact application-wide UI language switcher (M5.1.3).
 *
 * Shared by both CustomerLayout and AdminLayout via the shared
 * `app-header` — language is application state, not role-specific.
 * Deliberately small/unobtrusive (PL | EN toggle), matching the existing
 * header styling rather than introducing a new design pattern.
 */
@Component({
  selector: 'app-language-switcher',
  standalone: true,
  imports: [TranslocoModule],
  templateUrl: './language-switcher.component.html',
  styleUrl: './language-switcher.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LanguageSwitcherComponent {
  private readonly languageService = inject(LanguageService);
  private readonly translocoService = inject(TranslocoService);

  protected readonly languages = AVAILABLE_LANGUAGES;

  protected readonly activeLang = toSignal(this.translocoService.langChanges$, {
    initialValue: this.translocoService.getActiveLang(),
  });

  protected selectLanguage(language: AppLanguage): void {
    this.languageService.setLanguage(language);
  }
}
