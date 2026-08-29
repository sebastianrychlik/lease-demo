import { computed, Injectable, inject } from '@angular/core';

import { AuthService } from '../../services/auth.service';
import { AppRole, mapKeycloakRolesToAppRoles } from '../models/app-role.model';

/**
 * Centralized LeaseDemo application-role resolution.
 *
 * This is the ONE place components/guards ask "is this user an Admin?" /
 * "is this user a Customer?" / "what is their default landing area?".
 * Nothing else in the frontend should independently parse
 * `AuthService.roles()` or Keycloak token claims.
 *
 * Multi-role precedence (documented, deterministic — no role-switcher UI):
 *   A user with BOTH the ADMIN and CUSTOMER application roles is treated as
 *   ADMIN for the purpose of the default landing area. This is a demo-scope
 *   simplification, not a general authorization statement — see
 *   `RoleGuard`/`customerAreaGuard` for how CUSTOMER-only areas are enforced
 *   independently of the landing-area decision.
 *
 * Fail-closed behavior:
 *   An authenticated user whose Keycloak realm roles map to NO recognized
 *   {@link AppRole} is granted neither Admin nor Customer access. Callers
 *   must route such users to /access-denied rather than defaulting them
 *   into either application area.
 */
@Injectable({ providedIn: 'root' })
export class RoleService {
  private readonly authService = inject(AuthService);

  /** All recognized LeaseDemo application roles for the current session. */
  readonly appRoles = computed<AppRole[]>(() =>
    mapKeycloakRolesToAppRoles(this.authService.roles())
  );

  /** True when the current user has the Admin application role. */
  readonly isAdmin = computed<boolean>(() => this.appRoles().includes(AppRole.Admin));

  /** True when the current user has the Customer application role. */
  readonly isCustomer = computed<boolean>(() => this.appRoles().includes(AppRole.Customer));

  /**
   * True when the authenticated user has no recognized LeaseDemo
   * application role at all (fail-closed case).
   */
  readonly hasNoRecognizedRole = computed<boolean>(() => this.appRoles().length === 0);

  /**
   * Resolves the default application-area landing route for the current
   * user, applying the documented ADMIN-wins precedence rule.
   *
   * Returns `null` when the user has no recognized application role —
   * callers must treat this as "route to /access-denied", never as a
   * silent fallback to either area.
   */
  resolveLandingRoute(): string | null {
    if (this.isAdmin()) {
      return '/admin/dashboard';
    }
    if (this.isCustomer()) {
      return '/customer/dashboard';
    }
    return null;
  }
}
