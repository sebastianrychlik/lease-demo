import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { RoleService } from '../services/role.service';

/**
 * Role guards — UX/navigation protection only.
 *
 * IMPORTANT: These guards select the correct application shell/navigation
 * for a good user experience. They are NOT the authoritative security
 * boundary — Spring Security (JWT `ROLE_*` authorization on `/api/**`)
 * remains the actual trust boundary protecting data and operations. Hiding
 * an Admin menu item or blocking an Angular route does not, by itself,
 * secure any backend API.
 *
 * These guards assume `authGuard` has already run on the parent route and
 * guaranteed an authenticated session; they only add a role check on top.
 */

/** Allows navigation only for users holding the ADMIN application role. */
export const adminAreaGuard: CanActivateFn = () => {
  const roleService = inject(RoleService);
  const router = inject(Router);

  if (roleService.isAdmin()) {
    return true;
  }

  return router.parseUrl('/access-denied');
};

/**
 * Allows navigation only for users holding the CUSTOMER application role.
 *
 * Demo-scope policy (deliberately strict — see RoleService docs): the
 * Customer area requires the CUSTOMER role outright. An ADMIN user who also
 * holds CUSTOMER is naturally allowed; an ADMIN-only user is denied.
 */
export const customerAreaGuard: CanActivateFn = () => {
  const roleService = inject(RoleService);
  const router = inject(Router);

  if (roleService.isCustomer()) {
    return true;
  }

  return router.parseUrl('/access-denied');
};
