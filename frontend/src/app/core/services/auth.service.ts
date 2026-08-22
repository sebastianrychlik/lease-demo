import { Injectable, signal } from '@angular/core';
import Keycloak from 'keycloak-js';

/**
 * Authentication service — Keycloak integration.
 *
 * Responsibilities:
 * - Manages authentication state derived from the Keycloak adapter.
 * - Exposes reactive signals for authenticated status, username, and realm roles.
 * - Delegates login/logout to the Keycloak-hosted flow (Authorization Code + PKCE S256).
 * - Provides token retrieval with automatic near-expiry refresh.
 *
 * The Keycloak adapter is injected via {@link initialize}, called once during
 * application bootstrap by {@link keycloakInitializerFactory}.
 *
 * Security invariants:
 * - Angular never collects or sees the user's password.
 * - No client secret is present in this code.
 * - Token is refreshed via the Keycloak adapter, not validated here.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private keycloak: Keycloak | null = null;

  private readonly _isAuthenticated = signal<boolean>(false);
  private readonly _username = signal<string | undefined>(undefined);
  private readonly _roles = signal<string[]>([]);

  /** Reactive read-only signal: true when the user has an active Keycloak session. */
  readonly isAuthenticated = this._isAuthenticated.asReadonly();

  /** Reactive read-only signal: Keycloak preferred_username, or undefined when not authenticated. */
  readonly username = this._username.asReadonly();

  /**
   * Reactive read-only signal: realm roles from the access token.
   * Contains application roles (CUSTOMER, ADVISOR, ADMIN) and Keycloak default roles.
   */
  readonly roles = this._roles.asReadonly();

  /**
   * Initialises the service with the Keycloak adapter instance.
   *
   * Called once by {@link keycloakInitializerFactory} after Keycloak.init() completes.
   * Must not be called again; not part of the public API used by components.
   *
   * @internal
   */
  initialize(keycloak: Keycloak): void {
    this.keycloak = keycloak;

    const authenticated = keycloak.authenticated ?? false;
    this._isAuthenticated.set(authenticated);

    if (authenticated && keycloak.tokenParsed) {
      this._username.set(
        keycloak.tokenParsed['preferred_username'] as string | undefined
      );

      const realmAccess = keycloak.tokenParsed['realm_access'] as
        | { roles?: string[] }
        | undefined;
      this._roles.set(realmAccess?.roles ?? []);
    }
  }

  /**
   * Redirects the browser to the Keycloak-hosted login page.
   *
   * After successful authentication Keycloak redirects back to the provided
   * {@link redirectUri}.  Defaults to the application origin so the user
   * lands on the root route.
   *
   * @param redirectUri - Optional post-login destination. Falls back to origin.
   */
  login(redirectUri?: string): void {
    this.keycloak?.login({
      redirectUri: redirectUri ?? window.location.origin,
    });
  }

  /**
   * Logs the user out through Keycloak and redirects back to the application root.
   *
   * This performs a full Keycloak session termination — not just a local flag change.
   */
  logout(): void {
    this.keycloak?.logout({
      redirectUri: window.location.origin,
    });
  }

  /**
   * Returns the current access token, refreshing it first if it expires
   * within the next 30 seconds.
   *
   * Returns `undefined` when the user is not authenticated or when token
   * refresh fails (session expired).
   */
  async getToken(): Promise<string | undefined> {
    if (!this.keycloak?.authenticated) {
      return undefined;
    }

    try {
      // Refresh the token if it expires within 30 seconds
      await this.keycloak.updateToken(30);
    } catch {
      // Refresh failed — the session has expired; update local state
      this._isAuthenticated.set(false);
      this._username.set(undefined);
      this._roles.set([]);
      return undefined;
    }

    return this.keycloak.token;
  }
}
