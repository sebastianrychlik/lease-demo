import { Routes } from '@angular/router';

/**
 * Application root routes.
 *
 * Architecture:
 * - All feature routes use lazy loading via loadChildren.
 * - Each feature module lives under src/app/features/<feature-name>/.
 * - Layout wrappers can be applied per route group.
 */
export const APP_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'dashboard',
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./features/dashboard/dashboard-placeholder.component').then(
        (m) => m.DashboardPlaceholderComponent
      ),
  },
  {
    path: 'exchange-rates',
    loadChildren: () =>
      import('./features/exchange-rates/exchange-rates.routes').then(
        (m) => m.EXCHANGE_RATES_ROUTES
      ),
  },
  {
    path: '**',
    loadComponent: () =>
      import('./shared/components/not-found/not-found.component').then(
        (m) => m.NotFoundComponent
      ),
  },
];
