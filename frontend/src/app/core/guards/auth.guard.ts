import { inject } from '@angular/core';
import { CanActivateFn } from '@angular/router';

import { AuthService } from '../services/auth.service';

/**
 * Authentication guard.
 *
 * Responsibilities:
 * - Allows navigation when the user has an active Keycloak session.
 * - For unauthenticated users, initiates the Keycloak Authorization Code + PKCE S256
 *   flow instead of routing to a local username/password page.
 * - Preserves the originally requested URL as the post-login redirect target so
 *   the user returns to the intended page after successful authentication.
 *
 * Usage:
 *   { path: 'protected', canActivate: [authGuard], ... }
 */
export const authGuard: CanActivateFn = (_route, state) => {
  const authService = inject(AuthService);

  if (authService.isAuthenticated()) {
    return true;
  }

  // Redirect to Keycloak with the originally requested path as redirectUri
  // so the user lands on the correct page after a successful login.
  authService.login(`${window.location.origin}${state.url}`);
  return false;
};
