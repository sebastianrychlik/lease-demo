import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';
import { adminAreaGuard, customerAreaGuard } from './core/auth/guards/role.guard';
import { rootRedirectGuard } from './core/auth/guards/root-redirect.guard';

/**
 * Application root routes.
 *
 * Architecture:
 * - All feature routes use lazy loading via loadChildren/loadComponent.
 * - Each feature module lives under src/app/features/<feature-name>/.
 * - Role-aware application areas: /admin/** (AdminLayout) and
 *   /customer/** (CustomerLayout). Neither is user-selectable — the
 *   authenticated user's Keycloak roles determine which area is reachable
 *   (see RoleService / role.guard.ts / root-redirect.guard.ts).
 *
 * Authentication:
 * - authGuard initiates the Keycloak Authorization Code + PKCE S256 flow
 *   for unauthenticated visitors instead of showing a local login form.
 * - Role guards (adminAreaGuard/customerAreaGuard) run AFTER authGuard and
 *   are UX/navigation protection only — Spring Security remains the
 *   authoritative API security boundary (see docs § M4.3).
 * - The wildcard (not-found) route is intentionally left unprotected.
 */
export const APP_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    canActivate: [authGuard, rootRedirectGuard],
    // rootRedirectGuard always redirects (never returns true) once
    // authenticated; this component is effectively unreachable but
    // required to satisfy the Routes type.
    loadComponent: () =>
      import('./shared/components/not-found/not-found.component').then(
        (m) => m.NotFoundComponent
      ),
  },
  {
    path: 'admin',
    canActivate: [authGuard, adminAreaGuard],
    loadChildren: () => import('./features/admin/admin.routes').then((m) => m.ADMIN_ROUTES),
  },
  {
    path: 'customer',
    canActivate: [authGuard, customerAreaGuard],
    loadChildren: () =>
      import('./features/customer/customer.routes').then((m) => m.CUSTOMER_ROUTES),
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
    // LeaseDemo's living design-system catalog (M4.2). Admin/developer-only
    // as of M4.3: reachable via AdminLayout's "UX Demo" navigation link.
    // The convenient /ux-demo URL is preserved directly (not nested under
    // /admin) rather than duplicated under another route.
    path: 'lease-quote',
    canActivate: [authGuard],
    loadChildren: () =>
      import('./features/lease-quote/lease-quote.routes').then((m) => m.LEASE_QUOTE_ROUTES),
  },
  {
    path: 'ux-demo',
    canActivate: [authGuard, adminAreaGuard],
    loadChildren: () =>
      import('./features/ux-demo/ux-demo.routes').then((m) => m.UX_DEMO_ROUTES),
  },
  {
    path: 'access-denied',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/access-denied/access-denied-page.component').then(
        (m) => m.AccessDeniedPageComponent
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
