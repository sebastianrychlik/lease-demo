import { DecimalPipe } from '@angular/common';
import { Pipe, PipeTransform } from '@angular/core';

/**
 * Locale-aware decimal number formatter (M5.1.3.1).
 *
 * Thin, pure wrapper around Angular's native {@link DecimalPipe} — used for
 * plain (non-monetary) numeric values such as the NBP exchange rate, where
 * the source precision must be preserved rather than rounded to 2 decimals.
 *
 * Usage: `{{ rate | ldNumber:locale():'1.2-4' }}`
 */
@Pipe({
  name: 'ldNumber',
  standalone: true,
  pure: true,
})
export class LdNumberPipe implements PipeTransform {
  private readonly decimalPipe = new DecimalPipe('en-US');

  transform(value: number | null | undefined, locale: string, digitsInfo = '1.0-3'): string | null {
    if (value === null || value === undefined) {
      return null;
    }
    return this.decimalPipe.transform(value, digitsInfo, locale);
  }
}
