import { Routes } from '@angular/router';

/**
 * Lease Quote Simulator feature routes (M5.1).
 *
 * Lazily loaded when the user navigates to /lease-quote.
 */
export const LEASE_QUOTE_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/lease-quote-page.component').then((m) => m.LeaseQuotePageComponent),
  },
];
