import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';

import { authGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';

describe('authGuard', () => {
  const mockRoute = {} as ActivatedRouteSnapshot;
  const mockState = { url: '/dashboard' } as RouterStateSnapshot;

  let isAuthenticatedValue: boolean;
  let loginSpy: jasmine.Spy;

  function executeGuard(): boolean | unknown {
    return TestBed.runInInjectionContext(() => authGuard(mockRoute, mockState));
  }

  beforeEach(() => {
    isAuthenticatedValue = false;
    loginSpy = jasmine.createSpy('login');

    TestBed.configureTestingModule({
      providers: [
        {
          provide: AuthService,
          useValue: {
            isAuthenticated: () => isAuthenticatedValue,
            login: loginSpy,
          },
        },
      ],
    });
  });

  it('should permit navigation when the user is authenticated', () => {
    isAuthenticatedValue = true;
    expect(executeGuard()).toBeTrue();
  });

  it('should block navigation when the user is unauthenticated', () => {
    isAuthenticatedValue = false;
    expect(executeGuard()).toBeFalse();
  });

  it('should trigger the Keycloak login flow when unauthenticated', () => {
    isAuthenticatedValue = false;
    executeGuard();
    expect(loginSpy).toHaveBeenCalled();
  });

  it('should NOT trigger login when the user is already authenticated', () => {
    isAuthenticatedValue = true;
    executeGuard();
    expect(loginSpy).not.toHaveBeenCalled();
  });

  it('should include the originally requested path in the login redirectUri', () => {
    isAuthenticatedValue = false;
    executeGuard();
    // The guard passes `${window.location.origin}${state.url}` to login().
    const calledWith = loginSpy.calls.mostRecent().args[0] as string;
    expect(calledWith).toContain('/dashboard');
  });
});
