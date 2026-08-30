import { inject, Injectable } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';
import { LeaseProductConfiguration } from '../models/lease-quote.model';

/**
 * Lease product configuration service (M5.1.2).
 *
 * Responsibilities:
 * - Calls the backend Lease Product availability endpoint through ApiService.
 * - Exposes a single shared `Observable<LeaseProductConfiguration[]>` so
 *   multiple consumers (form defaults, option rendering) never trigger
 *   duplicate HTTP calls.
 * - Contains no business logic — the backend is the sole authority on
 *   which products/options exist (currencies, terms, ranges, lease types,
 *   defaults). The frontend renders this configuration, it does not own it.
 *
 * Endpoint: GET /api/lease-products/available
 * The current market is resolved entirely server-side — this service never
 * sends a market parameter.
 */
@Injectable({ providedIn: 'root' })
export class LeaseProductService {
  private readonly api = inject(ApiService);

  /**
   * Shared, replayed stream of available Lease Products for the current
   * (server-resolved) market. `shareReplay({ bufferSize: 1, refCount: true })`
   * ensures the HTTP request is made once regardless of how many consumers
   * subscribe.
   */
  readonly availableProducts$: Observable<LeaseProductConfiguration[]> = this.api
    .get<LeaseProductConfiguration[]>('/lease-products/available')
    .pipe(shareReplay({ bufferSize: 1, refCount: true }));
}
