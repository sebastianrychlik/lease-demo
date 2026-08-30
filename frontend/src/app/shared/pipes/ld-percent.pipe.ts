import { DecimalPipe } from '@angular/common';
import { Pipe, PipeTransform } from '@angular/core';

/**
 * Locale-aware percentage formatter (M5.1.3.1).
 *
 * IMPORTANT SEMANTICS: unlike Angular's native `PercentPipe` (which expects
 * a *fraction*, e.g. `0.072` → `7%`), the backend's percentage fields (e.g.
 * `annualRatePercent`) are already expressed in PERCENTAGE POINTS
 * (`7.20` meaning "7.20 percent"). This pipe therefore delegates to
 * {@link DecimalPipe} (NOT `PercentPipe`) so `7.20` renders as `7,20%` (PL)
 * / `7.20%` (EN) instead of being multiplied into `720%`.
 *
 * Backend percentage semantics are never changed to accommodate this pipe.
 *
 * Usage: `{{ apr | ldPercent:locale() }}`
 */
@Pipe({
  name: 'ldPercent',
  standalone: true,
  pure: true,
})
export class LdPercentPipe implements PipeTransform {
  private readonly decimalPipe = new DecimalPipe('en-US');

  transform(value: number | null | undefined, locale: string, digitsInfo = '1.2-2'): string | null {
    if (value === null || value === undefined) {
      return null;
    }
    const formatted = this.decimalPipe.transform(value, digitsInfo, locale);
    return formatted === null ? null : `${formatted}%`;
  }
}
