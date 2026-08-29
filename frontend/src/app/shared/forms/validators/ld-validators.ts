import { AbstractControl, FormGroup, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Reusable, framework-level validation primitives (M5.1.4 corrective pass).
 *
 * These validators know NOTHING about Lease Product business rules — they
 * only express generic numeric/date/membership constraints. Feature forms
 * (e.g. the ADMIN Lease Product editor) compose these primitives with their
 * own field names. Every validator returns a STRUCTURED error object keyed
 * by a stable, semantic error key (never a hard-coded message) so message
 * resolution/translation stays centralized (see `resolveValidationMessage`
 * in `../validation-message`).
 *
 * Cross-field validators here follow one convention: they run at the
 * `FormGroup` level (so they can compare sibling controls) but attach their
 * resulting error to the most relevant CHILD control (e.g. the "max" field
 * for a min/max order violation), merging with — not clobbering — any
 * existing errors already set on that control by its own validators.
 */

function isEmpty(value: unknown): boolean {
  return value === null || value === undefined || value === ('' as unknown);
}

function setError(control: AbstractControl | null | undefined, key: string, value: unknown): void {
  if (!control) {
    return;
  }
  const errors = { ...(control.errors ?? {}) };
  errors[key] = value;
  control.setErrors(errors);
}

function clearError(control: AbstractControl | null | undefined, key: string): void {
  if (!control || !control.errors || !(key in control.errors)) {
    return;
  }
  const { [key]: _removed, ...rest } = control.errors;
  control.setErrors(Object.keys(rest).length ? rest : null);
}

/** Rejects negative numbers. `null`/`undefined`/empty values are considered valid (pair with `Validators.required`). */
export function nonNegativeValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (isEmpty(control.value)) {
      return null;
    }
    return control.value < 0 ? { nonNegative: true } : null;
  };
}

/** Requires a strictly positive number (`> 0`). */
export function positiveValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (isEmpty(control.value)) {
      return null;
    }
    return control.value > 0 ? null : { positive: true };
  };
}

/** Rejects values greater than `max`. */
export function maxValueValidator(max: number): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (isEmpty(control.value)) {
      return null;
    }
    return control.value <= max ? null : { maxValue: { max, actual: control.value } };
  };
}

/** Requires an integer within `[min, max]` (inclusive). */
export function integerRangeValidator(min: number, max: number): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (isEmpty(control.value)) {
      return null;
    }
    const value = control.value;
    if (!Number.isInteger(value) || value < min || value > max) {
      return { integerRange: { min, max, actual: value } };
    }
    return null;
  };
}

/** Requires the control's value to be present in a dynamically-computed list of allowed values. */
export function membershipValidator(allowedValuesGetter: () => readonly unknown[]): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (isEmpty(control.value)) {
      return null;
    }
    const allowed = allowedValuesGetter();
    return allowed.includes(control.value) ? null : { notInAllowedValues: { allowed, actual: control.value } };
  };
}

/** Rejects a value already present in a dynamically-computed list (e.g. duplicate lease terms). */
export function duplicateValidator(existingValuesGetter: () => readonly unknown[]): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (isEmpty(control.value)) {
      return null;
    }
    return existingValuesGetter().includes(control.value) ? { duplicate: true } : null;
  };
}

/**
 * FormGroup-level cross-field validator: `maxControlName`'s value must not
 * be lower than `minControlName`'s value. Attaches `rangeOrder` to the max
 * control when violated.
 */
export function rangeOrderValidator(minControlName: string, maxControlName: string): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const maxControl = (group as FormGroup).get(maxControlName);
    const min = (group as FormGroup).get(minControlName)?.value;
    const max = maxControl?.value;
    if (isEmpty(min) || isEmpty(max)) {
      clearError(maxControl, 'rangeOrder');
      return null;
    }
    if (max < min) {
      setError(maxControl, 'rangeOrder', true);
    } else {
      clearError(maxControl, 'rangeOrder');
    }
    return null;
  };
}

/**
 * FormGroup-level cross-field validator: `defaultControlName`'s value must
 * lie within `[minControlName, maxControlName]`. Attaches
 * `defaultOutsideRange` to the default control when violated.
 */
export function defaultOutsideRangeValidator(
  minControlName: string,
  maxControlName: string,
  defaultControlName: string,
): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const defaultControl = (group as FormGroup).get(defaultControlName);
    const min = (group as FormGroup).get(minControlName)?.value;
    const max = (group as FormGroup).get(maxControlName)?.value;
    const value = defaultControl?.value;
    if (isEmpty(min) || isEmpty(max) || isEmpty(value)) {
      clearError(defaultControl, 'defaultOutsideRange');
      return null;
    }
    if (value < min || value > max) {
      setError(defaultControl, 'defaultOutsideRange', { min, max, actual: value });
    } else {
      clearError(defaultControl, 'defaultOutsideRange');
    }
    return null;
  };
}

/**
 * FormGroup-level cross-field validator: `toControlName`'s date must not be
 * earlier than `fromControlName`'s date. Null boundaries are always valid.
 * Attaches `dateOrder` to the "to" control when violated.
 */
export function dateOrderValidator(fromControlName: string, toControlName: string): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const toControl = (group as FormGroup).get(toControlName);
    const from = (group as FormGroup).get(fromControlName)?.value;
    const to = toControl?.value;
    if (!from || !to) {
      clearError(toControl, 'dateOrder');
      return null;
    }
    if (new Date(from).getTime() > new Date(to).getTime()) {
      setError(toControl, 'dateOrder', true);
    } else {
      clearError(toControl, 'dateOrder');
    }
    return null;
  };
}

/**
 * FormGroup-level validator for a nested boolean "enabled map" FormGroup
 * (e.g. currencies, lease types): requires at least one entry to be `true`.
 * Sets `atLeastOne` on the nested group itself.
 */
export function atLeastOneEnabledValidator(groupControlName: string): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const enabledGroup = (group as FormGroup).get(groupControlName) as FormGroup | null;
    if (!enabledGroup) {
      return null;
    }
    const anyTrue = Object.values(enabledGroup.controls).some((c) => c.value === true);
    if (anyTrue) {
      clearError(enabledGroup, 'atLeastOne');
    } else {
      setError(enabledGroup, 'atLeastOne', true);
    }
    return null;
  };
}

/**
 * FormGroup-level cross-field validator: `targetControlName`'s value must
 * be one of the keys currently set to `true` inside the sibling boolean
 * "enabled map" FormGroup named `enabledGroupControlName`. Sets
 * `notInAllowedValues` on the target control when violated.
 */
export function selectedInEnabledGroupValidator(
  enabledGroupControlName: string,
  targetControlName: string,
): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const enabledGroup = (group as FormGroup).get(enabledGroupControlName) as FormGroup | null;
    const targetControl = (group as FormGroup).get(targetControlName);
    if (!enabledGroup || !targetControl || isEmpty(targetControl.value)) {
      return null;
    }
    const isEnabled = enabledGroup.get(String(targetControl.value))?.value === true;
    if (isEnabled) {
      clearError(targetControl, 'notInAllowedValues');
    } else {
      setError(targetControl, 'notInAllowedValues', { actual: targetControl.value });
    }
    return null;
  };
}
