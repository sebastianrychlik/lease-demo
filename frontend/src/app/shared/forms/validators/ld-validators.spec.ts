import { FormControl, FormGroup } from '@angular/forms';

import {
  dateOrderValidator,
  duplicateValidator,
  integerRangeValidator,
  maxValueValidator,
  nonNegativeValidator,
  positiveValidator,
} from './ld-validators';

describe('nonNegativeValidator', () => {
  const validator = nonNegativeValidator();

  it('rejects -1', () => {
    expect(validator(new FormControl(-1))).toEqual({ nonNegative: true });
  });

  it('accepts 0', () => {
    expect(validator(new FormControl(0))).toBeNull();
  });
});

describe('positiveValidator', () => {
  const validator = positiveValidator();

  it('rejects 0', () => {
    expect(validator(new FormControl(0))).toEqual({ positive: true });
  });

  it('accepts 0.01', () => {
    expect(validator(new FormControl(0.01))).toBeNull();
  });
});

describe('maxValueValidator(100)', () => {
  const validator = maxValueValidator(100);

  it('accepts 100', () => {
    expect(validator(new FormControl(100))).toBeNull();
  });

  it('rejects 100.01', () => {
    expect(validator(new FormControl(100.01))).toEqual({ maxValue: { max: 100, actual: 100.01 } });
  });
});

describe('integerRangeValidator(6, 120)', () => {
  const validator = integerRangeValidator(6, 120);

  it('rejects -4', () => {
    expect(validator(new FormControl(-4))).toEqual({ integerRange: { min: 6, max: 120, actual: -4 } });
  });

  it('rejects 5', () => {
    expect(validator(new FormControl(5))?.['integerRange']).toBeTruthy();
  });

  it('accepts 6', () => {
    expect(validator(new FormControl(6))).toBeNull();
  });

  it('accepts 120', () => {
    expect(validator(new FormControl(120))).toBeNull();
  });

  it('rejects 121', () => {
    expect(validator(new FormControl(121))?.['integerRange']).toBeTruthy();
  });

  it('rejects 24.5 (non-integer)', () => {
    expect(validator(new FormControl(24.5))?.['integerRange']).toBeTruthy();
  });

  it('accepts 51 (the reported runtime regression value)', () => {
    expect(validator(new FormControl(51))).toBeNull();
  });
});

describe('duplicateValidator — external-dependency revalidation lifecycle', () => {
  it('flags a value present in the current external list, and clears once revalidated against an updated list', () => {
    let terms = [24, 36];
    const control = new FormControl<number | null>(24, { validators: [duplicateValidator(() => terms)] });

    expect(control.errors).toEqual({ duplicate: true });

    // Simulate `removeTerm(24)` — the external list changes but the control
    // itself is untouched; only `updateValueAndValidity` re-runs the validator.
    terms = [36];
    control.updateValueAndValidity();

    expect(control.errors).toBeNull();
  });

  it('clears immediately when the control value itself changes to a non-duplicate', () => {
    const terms = [24, 36];
    const control = new FormControl<number | null>(24, { validators: [duplicateValidator(() => terms)] });

    expect(control.errors).toEqual({ duplicate: true });

    control.setValue(51);

    expect(control.errors).toBeNull();
  });
});

describe('dateOrderValidator', () => {
  function group(from: string | null, to: string | null): FormGroup {
    return new FormGroup({
      validFrom: new FormControl(from),
      validTo: new FormControl(to),
    });
  }

  it('marks the "to" control invalid when from > to', () => {
    const g = group('2026-09-06', '2026-06-09');
    dateOrderValidator('validFrom', 'validTo')(g);
    expect(g.get('validTo')?.errors).toEqual({ dateOrder: true });
  });

  it('is valid when from <= to', () => {
    const g = group('2026-06-09', '2026-09-06');
    dateOrderValidator('validFrom', 'validTo')(g);
    expect(g.get('validTo')?.errors).toBeNull();
  });

  it('is valid when both boundaries are null', () => {
    const g = group(null, null);
    dateOrderValidator('validFrom', 'validTo')(g);
    expect(g.get('validTo')?.errors).toBeNull();
  });

  it('is valid when only one boundary is set', () => {
    const g = group('2026-06-09', null);
    dateOrderValidator('validFrom', 'validTo')(g);
    expect(g.get('validTo')?.errors).toBeNull();
  });
});
