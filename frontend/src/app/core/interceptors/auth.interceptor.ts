import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { from, switchMap } from 'rxjs';

import { AuthService } from '../services/auth.service';
import { environment } from '../../../environments/environment';

/**
 * Bearer-token HTTP interceptor.
 *
 * Responsibilities:
 * - Attaches an `Authorization: Bearer <token>` header to outgoing requests
 *   that target the LeaseDemo backend (`/api` prefix).
 * - Refreshes the Keycloak token if it is close to expiry before using it.
 * - Passes all other requests (external APIs, CDNs, etc.) through unchanged
 *   so the LeaseDemo Keycloak token is never sent to third-party services.
 *
 * Security invariant: only URLs starting with {@link environment.apiBaseUrl}
 * receive the bearer token.
 *
 * Registered in app.config.ts via withInterceptors([..., authInterceptor]).
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // Guard: do NOT attach the bearer token to requests outside the LeaseDemo backend.
  if (!req.url.startsWith(environment.apiBaseUrl)) {
    return next(req);
  }

  const authService = inject(AuthService);

  return from(authService.getToken()).pipe(
    switchMap((token) => {
      if (!token) {
        // Not authenticated — forward the request as-is; the backend will respond with 401.
        return next(req);
      }

      return next(
        req.clone({
          setHeaders: { Authorization: `Bearer ${token}` },
        })
      );
    })
  );
};
