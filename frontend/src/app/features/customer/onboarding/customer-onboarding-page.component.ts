import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import {
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { provideNativeDateAdapter } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Router } from '@angular/router';

import { ButtonComponent, CardComponent, InputComponent, PageHeaderComponent } from '../../../shared/ui';
import { CustomerCreateRequest } from '../profile/models/customer-profile.model';
import { CurrentCustomerService } from '../profile/services/current-customer.service';
import { peselChecksumValidator, peselCrossFieldValidator } from '../profile/validators/pesel.validator';

interface OnboardingFormControls {
  firstName: FormControl<string>;
  lastName: FormControl<string>;
  dateOfBirth: FormControl<Date | null>;
  gender: FormControl<'MALE' | 'FEMALE' | null>;
  pesel: FormControl<string>;
  email: FormControl<string>;
  phoneNumber: FormControl<string>;
}

/**
 * Customer Onboarding — /customer/onboarding (M4.5).
 *
 * Shown exactly once, to a CUSTOMER-authenticated JWT subject that has no
 * persisted Customer domain profile yet (`GET /api/customers/me` returned
 * 404). Submitting calls `POST /api/customers` — the backend derives the
 * owning Keycloak identity from the JWT, never from this form.
 *
 * Angular Reactive Forms + client-side validation exist for UX only; the
 * Spring Boot backend independently re-validates everything (structural
 * PESEL, checksum, DOB/gender cross-check, uniqueness) and is the
 * authoritative trust boundary. A 400 response from the backend is always
 * possible even when this form reports itself valid.
 */
@Component({
  selector: 'app-customer-onboarding-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    PageHeaderComponent,
    CardComponent,
    ButtonComponent,
    InputComponent,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
  ],
  providers: [provideNativeDateAdapter()],
  templateUrl: './customer-onboarding-page.component.html',
  styleUrl: './customer-onboarding-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CustomerOnboardingPageComponent {
  private readonly currentCustomerService = inject(CurrentCustomerService);
  private readonly router = inject(Router);

  readonly submitting = signal(false);
  readonly serverErrorMessage = signal<string | null>(null);
  readonly maxDateOfBirth = new Date();

  readonly form = new FormGroup<OnboardingFormControls>(
    {
      firstName: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
      lastName: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
      dateOfBirth: new FormControl<Date | null>(null, { validators: [Validators.required] }),
      gender: new FormControl<'MALE' | 'FEMALE' | null>(null, { validators: [Validators.required] }),
      pesel: new FormControl('', {
        nonNullable: true,
        validators: [Validators.required, peselChecksumValidator()],
      }),
      email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] }),
      phoneNumber: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    },
    { validators: [peselCrossFieldValidator('pesel', 'dateOfBirth', 'gender')] },
  );

  get peselErrorMessage(): string | null {
    const control = this.form.controls.pesel;
    if (!control.touched && !this.submitting()) {
      return null;
    }
    if (control.hasError('required')) {
      return 'PESEL is required.';
    }
    if (control.hasError('peselFormat')) {
      return 'PESEL must be exactly 11 digits.';
    }
    if (control.hasError('peselChecksum')) {
      return 'Invalid PESEL checksum.';
    }
    if (this.form.hasError('peselDobMismatch')) {
      return 'PESEL birth date does not match the selected date of birth.';
    }
    if (this.form.hasError('peselGenderMismatch')) {
      return 'PESEL gender does not match the selected gender.';
    }
    return null;
  }

  async onSubmit(): Promise<void> {
    this.serverErrorMessage.set(null);
    this.form.markAllAsTouched();

    if (this.form.invalid) {
      return;
    }

    const value = this.form.getRawValue();
    const dateOfBirth = value.dateOfBirth as Date;

    const request: CustomerCreateRequest = {
      firstName: value.firstName,
      lastName: value.lastName,
      email: value.email,
      phoneNumber: value.phoneNumber,
      dateOfBirth: toIsoDate(dateOfBirth),
      gender: value.gender as 'MALE' | 'FEMALE',
      pesel: value.pesel,
    };

    this.submitting.set(true);
    try {
      await this.currentCustomerService.createProfile(request);
      await this.router.navigateByUrl('/customer/dashboard');
    } catch (error) {
      this.serverErrorMessage.set(mapServerError(error));
    } finally {
      this.submitting.set(false);
    }
  }
}

/** Serializes a calendar date as `YYYY-MM-DD` without timezone conversion. */
function toIsoDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

/**
 * Maps a backend error into a safe, user-facing message. Never surfaces
 * raw Spring exception text/stack traces.
 */
function mapServerError(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 409) {
      return 'A profile already exists for your account, or this PESEL is already registered.';
    }
    if (error.status === 400) {
      return 'Some of the information provided is invalid. Please review the form and try again.';
    }
    if (error.status === 401) {
      return 'Your session has expired. Please sign in again.';
    }
  }
  return 'Something went wrong while completing your profile. Please try again later.';
}
