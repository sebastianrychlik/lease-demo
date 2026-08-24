import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { CurrentCustomerService } from '../services/current-customer.service';

/**
 * Refreshes the current Customer profile state before evaluating either
 * guard below, so a just-created profile (or a resolved backend hiccup) is
 * never trusted indefinitely across navigations.
 */
async function ensureProfileLoaded(service: CurrentCustomerService): Promise<void> {
  await service.load();
}

/**
 * Guards CUSTOMER routes that require a persisted domain profile
 * (dashboard, profile, leases, documents).
 *
 * - profile missing  -> redirect to /customer/onboarding
 * - profile found     -> allow
 * - error              -> allow through (page-level error UI handles it;
 *   we do not want a transient backend hiccup to trap the user in a
 *   redirect they cannot escape)
 */
export const requireCustomerProfileGuard: CanActivateFn = async () => {
  const service = inject(CurrentCustomerService);
  const router = inject(Router);

  await ensureProfileLoaded(service);

  if (service.state().status === 'missing') {
    return router.parseUrl('/customer/onboarding');
  }
  return true;
};

/**
 * Guards the onboarding route itself: a CUSTOMER who already has a
 * persisted profile should never see onboarding again — redirect to the
 * dashboard instead. This, combined with {@link requireCustomerProfileGuard}
 * redirecting the opposite direction, cannot loop because each guard only
 * redirects towards the route the other guard permits.
 */
export const onboardingGuard: CanActivateFn = async () => {
  const service = inject(CurrentCustomerService);
  const router = inject(Router);

  await ensureProfileLoaded(service);

  if (service.state().status === 'found') {
    return router.parseUrl('/customer/dashboard');
  }
  return true;
};
