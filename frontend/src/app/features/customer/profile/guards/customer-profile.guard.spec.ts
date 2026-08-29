import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, UrlTree } from '@angular/router';

import { CurrentCustomerService } from '../services/current-customer.service';
import { onboardingGuard, requireCustomerProfileGuard } from './customer-profile.guard';

describe('customer profile guards', () => {
  function setup(status: 'found' | 'missing' | 'error') {
    const state =
      status === 'found'
        ? {
            status: 'found' as const,
            profile: {
              id: '1',
              firstName: 'Jan',
              lastName: 'Kowalski',
              email: 'jan@example.com',
              phoneNumber: null,
              dateOfBirth: '1944-05-14',
              gender: 'MALE' as const,
              createdAt: '2025-01-01T00:00:00Z',
            },
          }
        : { status };

    const currentCustomerServiceStub = {
      load: jasmine.createSpy('load').and.resolveTo(undefined),
      state: () => state,
    };

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: CurrentCustomerService, useValue: currentCustomerServiceStub },
      ],
    });

    return { currentCustomerServiceStub };
  }

  function runGuard(guardFn: typeof onboardingGuard) {
    return TestBed.runInInjectionContext(() =>
      guardFn({} as never, { url: '/customer/x' } as never),
    );
  }

  describe('requireCustomerProfileGuard', () => {
    it('redirects to onboarding when the profile is missing', async () => {
      setup('missing');
      const result = await runGuard(requireCustomerProfileGuard);
      expect(result instanceof UrlTree).toBeTrue();
      const router = TestBed.inject(Router);
      expect(router.serializeUrl(result as UrlTree)).toBe('/customer/onboarding');
    });

    it('allows navigation when the profile exists', async () => {
      setup('found');
      const result = await runGuard(requireCustomerProfileGuard);
      expect(result).toBeTrue();
    });

    it('allows navigation through on a transient error (does not trap the user)', async () => {
      setup('error');
      const result = await runGuard(requireCustomerProfileGuard);
      expect(result).toBeTrue();
    });
  });

  describe('onboardingGuard', () => {
    it('redirects to dashboard when a profile already exists (no double onboarding)', async () => {
      setup('found');
      const result = await runGuard(onboardingGuard);
      expect(result instanceof UrlTree).toBeTrue();
      const router = TestBed.inject(Router);
      expect(router.serializeUrl(result as UrlTree)).toBe('/customer/dashboard');
    });

    it('allows navigation when the profile is missing (onboarding needed)', async () => {
      setup('missing');
      const result = await runGuard(onboardingGuard);
      expect(result).toBeTrue();
    });
  });
});
