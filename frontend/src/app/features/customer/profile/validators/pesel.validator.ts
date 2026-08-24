import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Frontend PESEL validation — Polish national identification number.
 *
 * IMPORTANT: this is UX only. A malicious or buggy client could bypass
 * Angular entirely and call `POST /api/customers` directly, so
 * `PeselValidator` (Java) + `CustomerService` on the backend remain the
 * authoritative trust boundary (see M4.5 docs). This validator exists
 * purely to give the user immediate, friendly feedback before submission.
 *
 * Mirrors the backend `PeselValidator`:
 * - exactly 11 digits
 * - checksum digit valid
 * - encoded century/month/day forms a real calendar date
 * - encoded gender digit is consistent
 */

const PESEL_LENGTH = 11;
const CHECKSUM_WEIGHTS = [1, 3, 7, 9, 1, 3, 7, 9, 1, 3];

/** Validates PESEL structure (11 digits) and checksum only. */
export function peselChecksumValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = (control.value ?? '') as string;
    if (!value) {
      return null; // let `Validators.required` own the empty case
    }
    if (!/^\d{11}$/.test(value)) {
      return { peselFormat: true };
    }
    if (!isChecksumValid(value)) {
      return { peselChecksum: true };
    }
    return null;
  };
}

/**
 * Cross-field validator: PESEL-encoded date of birth must equal the
 * declared `dateOfBirth` control, and PESEL-encoded gender must equal the
 * declared `gender` control.
 *
 * Applied at the FormGroup level (not on the PESEL control alone) because
 * it depends on sibling controls. Errors are exposed on the group so the
 * page component can render a single, clear message per mismatch type.
 */
export function peselCrossFieldValidator(
  peselControlName: string,
  dateOfBirthControlName: string,
  genderControlName: string,
): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const peselControl = group.get(peselControlName);
    const dobControl = group.get(dateOfBirthControlName);
    const genderControl = group.get(genderControlName);

    const pesel = (peselControl?.value ?? '') as string;
    if (!pesel || !/^\d{11}$/.test(pesel) || !isChecksumValid(pesel)) {
      return null; // structural errors are already reported on the PESEL control itself
    }

    const errors: ValidationErrors = {};

    const encodedDob = extractDateOfBirth(pesel);
    const declaredDob = dobControl?.value as Date | string | null;
    if (encodedDob && declaredDob && !isSameCalendarDate(encodedDob, declaredDob)) {
      errors['peselDobMismatch'] = true;
    }

    const encodedGender = extractGender(pesel);
    const declaredGender = genderControl?.value as 'MALE' | 'FEMALE' | null;
    if (encodedGender && declaredGender && encodedGender !== declaredGender) {
      errors['peselGenderMismatch'] = true;
    }

    return Object.keys(errors).length > 0 ? errors : null;
  };
}

function isChecksumValid(pesel: string): boolean {
  let sum = 0;
  for (let i = 0; i < CHECKSUM_WEIGHTS.length; i++) {
    sum += CHECKSUM_WEIGHTS[i] * Number(pesel[i]);
  }
  const checksum = sum % 10;
  const expected = (10 - checksum) % 10;
  return expected === Number(pesel[10]);
}

/** Extracts the PESEL-encoded date of birth, or `null` if the encoding is invalid. */
function extractDateOfBirth(pesel: string): Date | null {
  const yy = Number(pesel.slice(0, 2));
  let month = Number(pesel.slice(2, 4));
  const day = Number(pesel.slice(4, 6));

  let century: number;
  const monthOffset = Math.floor(month / 20) * 20;
  switch (monthOffset) {
    case 0:
      century = 1900;
      break;
    case 20:
      century = 2000;
      break;
    case 40:
      century = 2100;
      break;
    case 60:
      century = 2200;
      break;
    case 80:
      century = 1800;
      break;
    default:
      return null;
  }
  month -= monthOffset;

  const date = new Date(century + yy, month - 1, day);
  const valid =
    date.getFullYear() === century + yy && date.getMonth() === month - 1 && date.getDate() === day;
  return valid ? date : null;
}

/** Extracts the PESEL-encoded gender: even 10th digit = FEMALE, odd = MALE. */
function extractGender(pesel: string): 'MALE' | 'FEMALE' {
  const genderDigit = Number(pesel[9]);
  return genderDigit % 2 === 0 ? 'FEMALE' : 'MALE';
}

/** Compares only the calendar date (year/month/day), ignoring time/timezone. */
function isSameCalendarDate(a: Date, b: Date | string): boolean {
  const dateB = typeof b === 'string' ? parseIsoDate(b) : b;
  if (!dateB) {
    return true; // nothing to compare against
  }
  return (
    a.getFullYear() === dateB.getFullYear() &&
    a.getMonth() === dateB.getMonth() &&
    a.getDate() === dateB.getDate()
  );
}

function parseIsoDate(value: string): Date | null {
  const match = /^(\d{4})-(\d{2})-(\d{2})/.exec(value);
  if (!match) {
    return null;
  }
  return new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3]));
}
