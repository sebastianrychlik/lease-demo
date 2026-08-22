import Keycloak from 'keycloak-js';

import { AuthService } from '../services/auth.service';
import { environment } from '../../../environments/environment';

/**
 * Keycloak APP_INITIALIZER factory.
 *
 * Creates and initialises a Keycloak adapter instance before the Angular
 * application renders any route.  Once initialised, the adapter is handed
 * to {@link AuthService} so the rest of the application can query
 * authentication state through a clean Angular API.
 *
 * Init options:
 * - `onLoad: 'check-sso'`   — does NOT force a redirect to the login page;
 *                              silently detects whether an SSO session exists.
 * - `pkceMethod: 'S256'`    — enforces PKCE with the SHA-256 code challenge.
 * - `silentCheckSsoRedirectUri` — avoids a full-page redirect during the SSO
 *                              check by using a tiny hidden iframe instead.
 *
 * Usage (registered in app.config.ts):
 * ```ts
 * {
 *   provide: APP_INITIALIZER,
 *   useFactory: keycloakInitializerFactory,
 *   deps: [AuthService],
 *   multi: true,
 * }
 * ```
 */
export function keycloakInitializerFactory(
  authService: AuthService
): () => Promise<void> {
  return async () => {
    const keycloak = new Keycloak({
      url: environment.keycloak.url,
      realm: environment.keycloak.realm,
      clientId: environment.keycloak.clientId,
    });

    await keycloak.init({
      onLoad: 'check-sso',
      pkceMethod: 'S256',
      silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
    });

    authService.initialize(keycloak);
  };
}
