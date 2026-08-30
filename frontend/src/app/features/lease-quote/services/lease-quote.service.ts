import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';
import { LeaseQuoteRequest, LeaseQuoteResponse } from '../models/lease-quote.model';

/**
 * Lease quote service.
 *
 * Responsibilities:
 * - Calls the backend lease quote calculation endpoint through ApiService.
 * - Exposes a typed Observable<LeaseQuoteResponse>.
 * - Contains no calculation logic — the backend is the sole calculation
 *   authority (M5.1).
 *
 * Endpoint: POST /api/lease-quotes/calculate
 */
@Injectable({ providedIn: 'root' })
export class LeaseQuoteService {
  private readonly api = inject(ApiService);

  calculate(request: LeaseQuoteRequest): Observable<LeaseQuoteResponse> {
    return this.api.post<LeaseQuoteResponse>('/lease-quotes/calculate', request);
  }
}
