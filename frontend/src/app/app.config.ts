import { APP_INITIALIZER, ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter, withComponentInputBinding, withViewTransitions } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideTransloco } from '@jsverse/transloco';

import { APP_ROUTES } from './app.routes';
import { loggingInterceptor } from './core/interceptors/logging.interceptor';
import { errorInterceptor } from './core/interceptors/error.interceptor';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { keycloakInitializerFactory } from './core/auth/keycloak.factory';
import { AuthService } from './core/services/auth.service';
import { TranslocoHttpLoader } from './core/i18n/transloco-http-loader';
import { LanguageService } from './core/i18n/language.service';
import { AVAILABLE_LANGUAGES, DEFAULT_LANGUAGE, FALLBACK_LANGUAGE } from './core/i18n/language.model';
import { environment } from '../environments/environment';

/** Resolves the persisted/default UI language before the app renders (M5.1.3). */
function languageInitializerFactory(languageService: LanguageService): () => void {
  return () => languageService.init();
}

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
    provideTransloco({
      config: {
        availableLangs: AVAILABLE_LANGUAGES as unknown as string[],
        defaultLang: DEFAULT_LANGUAGE,
        fallbackLang: FALLBACK_LANGUAGE,
        reRenderOnLangChange: true,
        prodMode: environment.production,
      },
      loader: TranslocoHttpLoader,
    }),
    // Keycloak must be fully initialised before any route guard or service
    // can query authentication state.
    {
      provide: APP_INITIALIZER,
      useFactory: keycloakInitializerFactory,
      deps: [AuthService],
      multi: true,
    },
    // Resolves the persisted/default UI language (localStorage → PL) before
    // the first render, avoiding raw translation-key flashes.
    {
      provide: APP_INITIALIZER,
      useFactory: languageInitializerFactory,
      deps: [LanguageService],
      multi: true,
    },
  ],
};
