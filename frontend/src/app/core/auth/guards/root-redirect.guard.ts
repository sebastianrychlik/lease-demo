import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { RoleService } from '../services/role.service';

/**
 * Root ('/') redirect guard.
 *
 * Resolves the authenticated user's default application area deterministically:
 * - ADMIN (or ADMIN+CUSTOMER)  -> /admin/dashboard
 * - CUSTOMER only              -> /customer/dashboard
 * - no recognized application role -> /access-denied (fail closed)
 *
 * Runs after `authGuard`, which guarantees an authenticated session before
 * this guard evaluates. No redirect loop is possible: every branch below
 * redirects to a route that does not itself depend on this guard.
 */
export const rootRedirectGuard: CanActivateFn = () => {
  const roleService = inject(RoleService);
  const router = inject(Router);

  const landingRoute = roleService.resolveLandingRoute();
  return router.parseUrl(landingRoute ?? '/access-denied');
};
