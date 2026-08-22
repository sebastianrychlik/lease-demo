import { TestBed } from '@angular/core/testing';
import Keycloak from 'keycloak-js';

import { AuthService } from './auth.service';

/** Creates a minimal Keycloak mock with the given authentication state. */
function createMockKeycloak(opts: {
  authenticated: boolean;
  token?: string;
  tokenParsed?: Record<string, unknown>;
}): Keycloak {
  return {
    authenticated: opts.authenticated,
    token: opts.token,
    tokenParsed: opts.tokenParsed,
    login: jasmine.createSpy('login'),
    logout: jasmine.createSpy('logout'),
    updateToken: jasmine
      .createSpy('updateToken')
      .and.returnValue(Promise.resolve(true)),
  } as unknown as Keycloak;
}

describe('AuthService', () => {
  let service: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AuthService);
  });

  // ─── Initial state ───────────────────────────────────────────────────────

  describe('initial state (before initialize())', () => {
    it('should be unauthenticated', () => {
      expect(service.isAuthenticated()).toBeFalse();
    });

    it('should expose no username', () => {
      expect(service.username()).toBeUndefined();
    });

    it('should expose an empty roles array', () => {
      expect(service.roles()).toEqual([]);
    });
  });

  // ─── Authenticated session ───────────────────────────────────────────────

  describe('after initialize() with an authenticated Keycloak instance', () => {
    let mockKeycloak: Keycloak;

    beforeEach(() => {
      mockKeycloak = createMockKeycloak({
        authenticated: true,
        token: 'mock-access-token',
        tokenParsed: {
          preferred_username: 'customer',
          realm_access: {
            roles: ['CUSTOMER', 'offline_access', 'uma_authorization'],
          },
        },
      });
      service.initialize(mockKeycloak);
    });

    it('should reflect authenticated state', () => {
      expect(service.isAuthenticated()).toBeTrue();
    });

    it('should expose the preferred_username from the token', () => {
      expect(service.username()).toBe('customer');
    });

    it('should expose realm roles from the token', () => {
      expect(service.roles()).toContain('CUSTOMER');
    });

    it('login() should delegate to the Keycloak adapter', () => {
      service.login();
      expect(mockKeycloak.login as jasmine.Spy).toHaveBeenCalled();
    });

    it('logout() should delegate to the Keycloak adapter', () => {
      service.logout();
      expect(mockKeycloak.logout as jasmine.Spy).toHaveBeenCalled();
    });

    it('getToken() should return the token after a successful refresh', async () => {
      const token = await service.getToken();
      expect(token).toBe('mock-access-token');
    });

    it('getToken() should return undefined when token refresh fails', async () => {
      (mockKeycloak.updateToken as jasmine.Spy).and.returnValue(
        Promise.reject(new Error('Session expired'))
      );
      const token = await service.getToken();
      expect(token).toBeUndefined();
    });

    it('should mark user as unauthenticated when token refresh fails', async () => {
      (mockKeycloak.updateToken as jasmine.Spy).and.returnValue(
        Promise.reject(new Error('Session expired'))
      );
      await service.getToken();
      expect(service.isAuthenticated()).toBeFalse();
    });
  });

  // ─── Unauthenticated session ─────────────────────────────────────────────

  describe('after initialize() with an unauthenticated Keycloak instance', () => {
    beforeEach(() => {
      service.initialize(createMockKeycloak({ authenticated: false }));
    });

    it('should reflect unauthenticated state', () => {
      expect(service.isAuthenticated()).toBeFalse();
    });

    it('should expose no username', () => {
      expect(service.username()).toBeUndefined();
    });

    it('getToken() should return undefined without calling updateToken', async () => {
      const token = await service.getToken();
      expect(token).toBeUndefined();
    });
  });
});
