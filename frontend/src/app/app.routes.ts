import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';

/**
 * Application root routes.
 *
 * Architecture:
 * - All feature routes use lazy loading via loadChildren.
 * - Each feature module lives under src/app/features/<feature-name>/.
 * - Layout wrappers can be applied per route group.
 *
 * Authentication:
 * - dashboard and exchange-rates require an active Keycloak session.
 * - authGuard initiates the Keycloak Authorization Code + PKCE S256 flow
 *   for unauthenticated visitors instead of showing a local login form.
 * - The wildcard (not-found) route is intentionally left unprotected.
 */
export const APP_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'dashboard',
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/dashboard/dashboard-placeholder.component').then(
        (m) => m.DashboardPlaceholderComponent
      ),
  },
  {
    path: 'exchange-rates',
    canActivate: [authGuard],
    loadChildren: () =>
      import('./features/exchange-rates/exchange-rates.routes').then(
        (m) => m.EXCHANGE_RATES_ROUTES
      ),
  },
  {
    // LeaseDemo's living design-system catalog (M4.2). Directly routable
    // for now because role-aware Admin/Customer shells do not exist yet.
    // FUTURE: once they do, /ux-demo must only be reachable from
    // Admin/developer-facing navigation, never Customer-facing navigation.
    path: 'ux-demo',
    canActivate: [authGuard],
    loadChildren: () =>
      import('./features/ux-demo/ux-demo.routes').then((m) => m.UX_DEMO_ROUTES),
  },
  {
    path: '**',
    loadComponent: () =>
      import('./shared/components/not-found/not-found.component').then(
        (m) => m.NotFoundComponent
      ),
  },
];
