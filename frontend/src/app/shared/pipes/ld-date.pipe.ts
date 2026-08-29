import { DatePipe } from '@angular/common';
import { Pipe, PipeTransform } from '@angular/core';

/** Matches a date-only ISO string, e.g. `2026-08-29` (no time/timezone component). */
const DATE_ONLY_PATTERN = /^(\d{4})-(\d{2})-(\d{2})$/;

/**
 * Locale-aware date formatter (M5.1.3.1).
 *
 * Thin, pure wrapper around Angular's native {@link DatePipe}.
 *
 * Date-only values (e.g. the NBP `exchangeRateDate`, `2026-08-29`) are
 * parsed as LOCAL calendar-date components rather than handed to `DatePipe`
 * as a raw string — `new Date('2026-08-29')` is parsed as UTC midnight by
 * the `Date` constructor, which can render as the *previous* day in
 * negative-UTC-offset timezones. Building the `Date` from explicit
 * year/month/day components keeps the calendar date stable regardless of
 * the viewer's timezone.
 *
 * Usage: `{{ exchangeRateDate | ldDate:locale() }}`
 */
@Pipe({
  name: 'ldDate',
  standalone: true,
  pure: true,
})
export class LdDatePipe implements PipeTransform {
  private readonly datePipe = new DatePipe('en-US');

  transform(value: string | Date | null | undefined, locale: string, format = 'shortDate'): string | null {
    if (value === null || value === undefined || value === '') {
      return null;
    }
    const date = this.toSafeDate(value);
    return this.datePipe.transform(date, format, undefined, locale);
  }

  private toSafeDate(value: string | Date): Date | string {
    if (typeof value !== 'string') {
      return value;
    }
    const dateOnlyMatch = DATE_ONLY_PATTERN.exec(value);
    if (!dateOnlyMatch) {
      return value;
    }
    const [, year, month, day] = dateOnlyMatch;
    return new Date(Number(year), Number(month) - 1, Number(day));
  }
}
