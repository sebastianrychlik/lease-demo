import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TranslocoTestingModule } from '@jsverse/transloco';

import { nonNegativeValidator } from '../../../../shared/forms';
import { LeaseApplicationComponent, LeaseApplicationFormControls } from './lease-application.component';

/**
 * M5.5.1 — Lease Application money field validation.
 *
 * Covers: negative income/obligations rejected, zero accepted, positive
 * accepted, and the Apply button remaining disabled while the form is
 * invalid — mirroring the real `applicationForm` built in
 * `LeaseQuotePageComponent`.
 */
describe('LeaseApplicationComponent — financial field validation', () => {
  let fixture: ComponentFixture<LeaseApplicationComponent>;

  function buildFormGroup(): FormGroup<LeaseApplicationFormControls> {
    return new FormGroup<LeaseApplicationFormControls>({
      monthlyNetIncome: new FormControl<number | null>(null, {
        validators: [Validators.required, Validators.min(0), nonNegativeValidator()],
      }),
      monthlyObligations: new FormControl<number | null>(null, {
        validators: [Validators.required, Validators.min(0), nonNegativeValidator()],
      }),
    });
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        LeaseApplicationComponent,
        NoopAnimationsModule,
        TranslocoTestingModule.forRoot({
          langs: {
            en: {
              application: { title: 'Lease application', apply: 'Apply', fields: { monthlyNetIncome: 'Monthly net income', monthlyObligations: 'Monthly obligations' } },
              validation: { required: 'This field is required.', nonNegative: 'Value cannot be negative.' },
            },
          },
          translocoConfig: { availableLangs: ['en'], defaultLang: 'en' },
          preloadLangs: true,
        }),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LeaseApplicationComponent);
    fixture.componentRef.setInput('formGroup', buildFormGroup());
    fixture.componentRef.setInput('canApply', false);
    fixture.componentRef.setInput('state', { status: 'idle' });
    fixture.componentRef.setInput('settlementCurrency', 'PLN');
    fixture.componentRef.setInput('locale', 'en-US');
    fixture.detectChanges();
  });

  function group(): FormGroup<LeaseApplicationFormControls> {
    return fixture.componentInstance.formGroup();
  }

  it('rejects negative monthly net income', () => {
    const control = group().controls.monthlyNetIncome;
    control.setValue(-31);
    expect(control.invalid).toBeTrue();
    expect(control.errors).toEqual(jasmine.objectContaining({ nonNegative: true }));
  });

  it('rejects negative monthly obligations', () => {
    const control = group().controls.monthlyObligations;
    control.setValue(-1);
    expect(control.invalid).toBeTrue();
    expect(control.errors).toEqual(jasmine.objectContaining({ nonNegative: true }));
  });

  it('accepts zero for both fields', () => {
    group().controls.monthlyNetIncome.setValue(0);
    group().controls.monthlyObligations.setValue(0);
    expect(group().controls.monthlyNetIncome.valid).toBeTrue();
    expect(group().controls.monthlyObligations.valid).toBeTrue();
  });

  it('accepts positive values for both fields', () => {
    group().controls.monthlyNetIncome.setValue(15000);
    group().controls.monthlyObligations.setValue(1000);
    expect(group().controls.monthlyNetIncome.valid).toBeTrue();
    expect(group().controls.monthlyObligations.valid).toBeTrue();
  });

  it('does not show an error message before the field is touched', () => {
    group().controls.monthlyNetIncome.setValue(-31);
    fixture.detectChanges();
    expect(fixture.componentInstance.errorMessage('monthlyNetIncome')).toBeUndefined();
  });

  it('shows a translated validation message once the invalid field is touched', () => {
    const control = group().controls.monthlyNetIncome;
    control.setValue(-31);
    control.markAsTouched();
    fixture.detectChanges();
    expect(fixture.componentInstance.errorMessage('monthlyNetIncome')).toBe('Value cannot be negative.');
  });

  it('shows the required message once an empty, touched field is checked', () => {
    const control = group().controls.monthlyObligations;
    control.markAsTouched();
    fixture.detectChanges();
    expect(fixture.componentInstance.errorMessage('monthlyObligations')).toBe('This field is required.');
  });

  it('keeps the Apply button disabled while canApply() is false (invalid/incomplete form)', () => {
    fixture.componentRef.setInput('canApply', false);
    fixture.detectChanges();
    const button: HTMLButtonElement = fixture.nativeElement.querySelector('button[type="button"]');
    expect(button.disabled).toBeTrue();
  });

  it('enables the Apply button once canApply() is true', () => {
    fixture.componentRef.setInput('canApply', true);
    fixture.detectChanges();
    const button: HTMLButtonElement = fixture.nativeElement.querySelector('button[type="button"]');
    expect(button.disabled).toBeFalse();
  });
});
