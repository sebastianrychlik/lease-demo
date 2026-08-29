import { TestBed } from '@angular/core/testing';
import { HttpRequest, HttpHandlerFn, HttpResponse } from '@angular/common/http';
import { firstValueFrom, of } from 'rxjs';

import { authInterceptor } from './auth.interceptor';
import { AuthService } from '../services/auth.service';

describe('authInterceptor', () => {
  const MOCK_TOKEN = 'mock-bearer-token';

  let getTokenSpy: jasmine.Spy;

  /** Captures each request passed to `next` so assertions can inspect headers. */
  function buildCapturingNext(
    captured: HttpRequest<unknown>[]
  ): HttpHandlerFn {
    return (req) => {
      captured.push(req);
      return of(new HttpResponse({ status: 200 }));
    };
  }

  function runInterceptor(
    url: string,
    captured: HttpRequest<unknown>[]
  ) {
    return TestBed.runInInjectionContext(() =>
      authInterceptor(new HttpRequest('GET', url), buildCapturingNext(captured))
    );
  }

  beforeEach(() => {
    getTokenSpy = jasmine
      .createSpy('getToken')
      .and.returnValue(Promise.resolve(MOCK_TOKEN));

    TestBed.configureTestingModule({
      providers: [
        {
          provide: AuthService,
          useValue: { getToken: getTokenSpy },
        },
      ],
    });
  });

  // ─── Requests to the LeaseDemo backend (/api) ────────────────────────────

  describe('requests to /api (LeaseDemo backend)', () => {
    it('should attach an Authorization Bearer header', async () => {
      const captured: HttpRequest<unknown>[] = [];
      await firstValueFrom(runInterceptor('/api/health', captured));

      expect(captured[0].headers.get('Authorization')).toBe(
        `Bearer ${MOCK_TOKEN}`
      );
    });

    it('should call getToken() to obtain/refresh the access token', async () => {
      await firstValueFrom(
        runInterceptor('/api/exchange-rates', [])
      );
      expect(getTokenSpy).toHaveBeenCalled();
    });

    it('should forward request without header when no token is available', async () => {
      getTokenSpy.and.returnValue(Promise.resolve(undefined));
      const captured: HttpRequest<unknown>[] = [];
      await firstValueFrom(runInterceptor('/api/health', captured));

      expect(captured[0].headers.get('Authorization')).toBeNull();
    });
  });

  // ─── Requests to external / non-API URLs ─────────────────────────────────

  describe('requests to external / non-API URLs', () => {
    it('should NOT attach an Authorization header to external HTTPS requests', async () => {
      const captured: HttpRequest<unknown>[] = [];
      await firstValueFrom(
        runInterceptor('https://external-api.example.com/data', captured)
      );
      expect(captured[0].headers.get('Authorization')).toBeNull();
    });

    it('should NOT call getToken() for external requests', async () => {
      await firstValueFrom(
        runInterceptor('https://external-api.example.com/data', [])
      );
      expect(getTokenSpy).not.toHaveBeenCalled();
    });

    it('should NOT attach an Authorization header to requests to other paths', async () => {
      // e.g. a CDN or analytics endpoint that doesn't start with /api
      const captured: HttpRequest<unknown>[] = [];
      await firstValueFrom(
        runInterceptor('https://cdn.example.com/assets/logo.png', captured)
      );
      expect(captured[0].headers.get('Authorization')).toBeNull();
    });
  });
});
