import { CurrencyPipe } from '@angular/common';
import { Pipe, PipeTransform } from '@angular/core';

/**
 * Locale-aware currency formatter (M5.1.3.1).
 *
 * Thin, pure wrapper around Angular's native {@link CurrencyPipe}. Contains
 * no formatting logic of its own — decimal/thousands separators and
 * currency placement are entirely delegated to Angular's `Intl`-backed
 * formatter for the given `locale`.
 *
 * Displays the currency CODE (e.g. `PLN`, `EUR`) rather than a locale symbol,
 * and always uses 2 decimal places for monetary values.
 *
 * Usage: `{{ amount | ldCurrency:'PLN':locale() }}`
 */
@Pipe({
  name: 'ldCurrency',
  standalone: true,
  pure: true,
})
export class LdCurrencyPipe implements PipeTransform {
  transform(value: number | null | undefined, currencyCode: string, locale: string): string | null {
    if (value === null || value === undefined) {
      return null;
    }

    return new Intl.NumberFormat(locale, {
      style: 'currency',
      currency: currencyCode,
      currencyDisplay: 'code',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(value);
  }
}
