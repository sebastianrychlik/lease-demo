import { FormControl, FormGroup } from '@angular/forms';

import { peselChecksumValidator, peselCrossFieldValidator } from './pesel.validator';

// 44051401359 -> valid PESEL for a MALE born 1944-05-14 (used across backend tests too).
const VALID_PESEL_MALE_1944_05_14 = '44051401359';
// 44051401342 -> same date, valid checksum, FEMALE (10th digit '4' is even).
// Derived deterministically for this test; see backend PeselValidatorTest for the algorithm.
const VALID_PESEL_FEMALE_1944_05_14 = '44051401342';

describe('peselChecksumValidator', () => {
  const validator = peselChecksumValidator();

  it('returns null for an empty value (required is a separate validator)', () => {
    expect(validator(new FormControl(''))).toBeNull();
  });

  it('returns peselFormat error for a non-11-digit value', () => {
    expect(validator(new FormControl('123'))).toEqual({ peselFormat: true });
  });

  it('returns peselFormat error for non-digit characters', () => {
    expect(validator(new FormControl('4405140135A'))).toEqual({ peselFormat: true });
  });

  it('returns peselChecksum error for an invalid checksum', () => {
    expect(validator(new FormControl('44051401350'))).toEqual({ peselChecksum: true });
  });

  it('returns null for a structurally valid PESEL', () => {
    expect(validator(new FormControl(VALID_PESEL_MALE_1944_05_14))).toBeNull();
  });
});

describe('peselCrossFieldValidator', () => {
  function buildGroup(pesel: string, dateOfBirth: Date | null, gender: 'MALE' | 'FEMALE' | null) {
    return new FormGroup(
      {
        pesel: new FormControl(pesel),
        dateOfBirth: new FormControl<Date | null>(dateOfBirth),
        gender: new FormControl<'MALE' | 'FEMALE' | null>(gender),
      },
      { validators: [peselCrossFieldValidator('pesel', 'dateOfBirth', 'gender')] },
    );
  }

  it('is valid when PESEL DOB and gender both match the declared values (MALE)', () => {
    const group = buildGroup(VALID_PESEL_MALE_1944_05_14, new Date(1944, 4, 14), 'MALE');
    expect(group.errors).toBeNull();
  });

  it('is valid when PESEL DOB and gender both match the declared values (FEMALE)', () => {
    const group = buildGroup(VALID_PESEL_FEMALE_1944_05_14, new Date(1944, 4, 14), 'FEMALE');
    expect(group.errors).toBeNull();
  });

  it('flags peselDobMismatch when the declared date of birth differs', () => {
    const group = buildGroup(VALID_PESEL_MALE_1944_05_14, new Date(1981, 4, 20), 'MALE');
    expect(group.hasError('peselDobMismatch')).toBeTrue();
  });

  it('flags peselGenderMismatch when the declared gender differs', () => {
    const group = buildGroup(VALID_PESEL_MALE_1944_05_14, new Date(1944, 4, 14), 'FEMALE');
    expect(group.hasError('peselGenderMismatch')).toBeTrue();
  });

  it('does not evaluate cross-field rules while the PESEL itself is structurally invalid', () => {
    const group = buildGroup('123', new Date(1981, 4, 20), 'FEMALE');
    expect(group.errors).toBeNull();
  });
});
