import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { Router, provideRouter } from '@angular/router';

import { CustomerOnboardingPageComponent } from './customer-onboarding-page.component';

const VALID_PESEL = '44051401359'; // MALE, 1944-05-14

describe('CustomerOnboardingPageComponent', () => {
  let fixture: ComponentFixture<CustomerOnboardingPageComponent>;
  let component: CustomerOnboardingPageComponent;
  let httpMock: HttpTestingController;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CustomerOnboardingPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideNoopAnimations(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CustomerOnboardingPageComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  afterEach(() => httpMock.verify());

  function fillValidForm(): void {
    component.form.setValue({
      firstName: 'Jan',
      lastName: 'Kowalski',
      dateOfBirth: new Date(1944, 4, 14),
      gender: 'MALE',
      pesel: VALID_PESEL,
      email: 'jan.kowalski@example.com',
      phoneNumber: '+48123456789',
    });
  }

  it('required fields are invalid when empty', () => {
    expect(component.form.invalid).toBeTrue();
    expect(component.form.controls.firstName.hasError('required')).toBeTrue();
    expect(component.form.controls.pesel.hasError('required')).toBeTrue();
  });

  it('rejects an invalid email format', () => {
    component.form.controls.email.setValue('not-an-email');
    expect(component.form.controls.email.hasError('email')).toBeTrue();
  });

  it('rejects a PESEL that is not 11 digits', () => {
    component.form.controls.pesel.setValue('123');
    expect(component.form.controls.pesel.hasError('peselFormat')).toBeTrue();
  });

  it('rejects a PESEL with an invalid checksum', () => {
    component.form.controls.pesel.setValue('44051401350');
    expect(component.form.controls.pesel.hasError('peselChecksum')).toBeTrue();
  });

  it('accepts a PESEL whose encoded date of birth matches the declared value', () => {
    fillValidForm();
    expect(component.form.hasError('peselDobMismatch')).toBeFalse();
  });

  it('rejects a PESEL/date-of-birth mismatch', () => {
    fillValidForm();
    component.form.controls.dateOfBirth.setValue(new Date(1981, 4, 20));
    expect(component.form.hasError('peselDobMismatch')).toBeTrue();
  });

  it('accepts a PESEL whose encoded gender matches the declared value', () => {
    fillValidForm();
    expect(component.form.hasError('peselGenderMismatch')).toBeFalse();
  });

  it('rejects a PESEL/gender mismatch', () => {
    fillValidForm();
    component.form.controls.gender.setValue('FEMALE');
    expect(component.form.hasError('peselGenderMismatch')).toBeTrue();
  });

  it('an invalid form does not submit (no HTTP call)', async () => {
    await component.onSubmit();
    httpMock.expectNone('/api/customers');
    expect(component.form.invalid).toBeTrue();
  });

  it('a valid form submits and navigates to the dashboard on success', async () => {
    fillValidForm();
    const navigateSpy = spyOn(router, 'navigateByUrl').and.resolveTo(true);

    const submitPromise = component.onSubmit();
    const req = httpMock.expectOne('/api/customers');
    expect(req.request.method).toBe('POST');
    expect((req.request.body as Record<string, unknown>)['keycloakUserId']).toBeUndefined();
    expect(req.request.body).toEqual({
      firstName: 'Jan',
      lastName: 'Kowalski',
      email: 'jan.kowalski@example.com',
      phoneNumber: '+48123456789',
      dateOfBirth: '1944-05-14',
      gender: 'MALE',
      pesel: VALID_PESEL,
    });
    req.flush({
      id: '1',
      firstName: 'Jan',
      lastName: 'Kowalski',
      email: 'jan.kowalski@example.com',
      phoneNumber: '+48123456789',
      dateOfBirth: '1944-05-14',
      gender: 'MALE',
      createdAt: '2025-01-01T00:00:00Z',
    });
    await submitPromise;

    expect(navigateSpy).toHaveBeenCalledWith('/customer/dashboard');
    expect(component.serverErrorMessage()).toBeNull();
  });

  it('displays a safe message on a backend 409 duplicate response', async () => {
    fillValidForm();
    const submitPromise = component.onSubmit();
    const req = httpMock.expectOne('/api/customers');
    req.flush(
      { title: 'Duplicate Customer', detail: 'A customer with this PESEL is already registered' },
      { status: 409, statusText: 'Conflict' },
    );
    await submitPromise;

    expect(component.serverErrorMessage()).toContain('already');
  });

  it('displays a safe message on a backend 400 validation response without leaking details', async () => {
    fillValidForm();
    const submitPromise = component.onSubmit();
    const req = httpMock.expectOne('/api/customers');
    req.flush(
      { title: 'Invalid PESEL', detail: 'Something internal and technical' },
      { status: 400, statusText: 'Bad Request' },
    );
    await submitPromise;

    expect(component.serverErrorMessage()).not.toContain('Something internal and technical');
  });
});
