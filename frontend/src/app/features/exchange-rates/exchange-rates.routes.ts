import { Routes } from '@angular/router';

/**
 * Exchange rates feature routes.
 *
 * Lazily loaded when the user navigates to /exchange-rates.
 */
export const EXCHANGE_RATES_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/exchange-rates-page.component').then(
        (m) => m.ExchangeRatesPageComponent
      ),
  },
];
