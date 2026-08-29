import { Routes } from '@angular/router';

import { CustomerLayoutComponent } from '../../layout/customer-layout/customer-layout.component';
import { onboardingGuard, requireCustomerProfileGuard } from './profile/guards/customer-profile.guard';

/**
 * Customer feature routes — all wrapped in {@link CustomerLayoutComponent}.
 *
 * Role protection (CUSTOMER-required) is applied once, on the parent
 * `/customer` route in app.routes.ts (`customerAreaGuard`).
 *
 * Profile-existence protection (M4.5) is applied per-route below:
 * - `onboarding` uses `onboardingGuard` (redirects to dashboard if a
 *   profile already exists — onboarding must not be shown twice).
 * - `dashboard` / `profile` / `leases` / `documents` use
 *   `requireCustomerProfileGuard` (redirects to onboarding if no profile
 *   exists yet). This cannot loop: the two guards redirect in opposite,
 *   non-overlapping directions.
 */
export const CUSTOMER_ROUTES: Routes = [
  {
    path: '',
    component: CustomerLayoutComponent,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'onboarding',
        canActivate: [onboardingGuard],
        loadComponent: () =>
          import('./onboarding/customer-onboarding-page.component').then(
            (m) => m.CustomerOnboardingPageComponent
          ),
      },
      {
        path: 'dashboard',
        canActivate: [requireCustomerProfileGuard],
        loadComponent: () =>
          import('./dashboard/customer-dashboard-page.component').then(
            (m) => m.CustomerDashboardPageComponent
          ),
      },
      {
        path: 'lease-quote',
        canActivate: [requireCustomerProfileGuard],
        loadChildren: () =>
          import('../lease-quote/lease-quote.routes').then((m) => m.LEASE_QUOTE_ROUTES),
      },
      {
        path: 'leases',
        canActivate: [requireCustomerProfileGuard],
        loadComponent: () =>
          import('../../shared/components/feature-placeholder/feature-placeholder.component').then(
            (m) => m.FeaturePlaceholderComponent
          ),
        data: { title: 'My leases' },
      },
      {
        path: 'documents',
        canActivate: [requireCustomerProfileGuard],
        loadComponent: () =>
          import('../../shared/components/feature-placeholder/feature-placeholder.component').then(
            (m) => m.FeaturePlaceholderComponent
          ),
        data: { title: 'Documents' },
      },
      {
        path: 'profile',
        canActivate: [requireCustomerProfileGuard],
        loadComponent: () =>
          import('./profile/pages/customer-profile-page.component').then(
            (m) => m.CustomerProfilePageComponent
          ),
      },
    ],
  },
];
