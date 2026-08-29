import { ValidationErrors } from '@angular/forms';
import { TranslocoService } from '@jsverse/transloco';

/**
 * Central Angular-validation-error → translation-key resolver (M5.1.4).
 *
 * Angular validators (built-in and the reusable ones in
 * `./validators/ld-validators`) attach STRUCTURED error objects to controls
 * (e.g. `{ integerRange: { min, max, actual } }`). This module is the single
 * place that knows how to turn those structured errors into a translated,
 * human-readable message — so neither the validators themselves nor
 * `app-input`/`app-select` need to know about PL/EN strings.
 *
 * Usage in a feature component:
 * ```ts
 * errorMessage(control: AbstractControl | null): string | undefined {
 *   return resolveValidationMessage(this.transloco, control?.errors);
 * }
 * ```
 *
 * Error keys are checked in a fixed, deliberate precedence order so a
 * control with multiple simultaneous errors always shows one stable,
 * predictable message.
 */
const ERROR_KEY_PRECEDENCE = [
  'required',
  'pattern',
  'nonNegative',
  'positive',
  'maxValue',
  'integerRange',
  'duplicate',
  'notInAllowedValues',
  'rangeOrder',
  'defaultOutsideRange',
  'dateOrder',
  'atLeastOne',
] as const;

/** Resolves the highest-precedence error on `errors` to a translated message, or `undefined` when there are none. */
export function resolveValidationMessage(
  transloco: TranslocoService,
  errors: ValidationErrors | null | undefined,
): string | undefined {
  if (!errors) {
    return undefined;
  }

  for (const key of ERROR_KEY_PRECEDENCE) {
    if (!(key in errors)) {
      continue;
    }
    const params = typeof errors[key] === 'object' ? (errors[key] as Record<string, unknown>) : {};
    return transloco.translate(`validation.${key}`, params);
  }

  // Unknown/unmapped error key — fall back to a generic message rather than
  // silently showing nothing.
  return transloco.translate('validation.invalid');
}
