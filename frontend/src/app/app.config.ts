import { APP_INITIALIZER, ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter, withComponentInputBinding, withViewTransitions } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';

import { APP_ROUTES } from './app.routes';
import { loggingInterceptor } from './core/interceptors/logging.interceptor';
import { errorInterceptor } from './core/interceptors/error.interceptor';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { keycloakInitializerFactory } from './core/auth/keycloak.factory';
import { AuthService } from './core/services/auth.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(
      APP_ROUTES,
      withComponentInputBinding(),
      withViewTransitions()
    ),
    provideHttpClient(
      // authInterceptor must run before errorInterceptor so the token is
      // present when the backend returns errors that require an auth header.
      withInterceptors([loggingInterceptor, authInterceptor, errorInterceptor])
    ),
    provideAnimationsAsync(),
    // Keycloak must be fully initialised before any route guard or service
    // can query authentication state.
    {
      provide: APP_INITIALIZER,
      useFactory: keycloakInitializerFactory,
      deps: [AuthService],
      multi: true,
    },
  ],
};
