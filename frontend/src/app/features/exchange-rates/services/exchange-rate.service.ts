import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';
import { ExchangeRateResponse } from '../models/exchange-rate.model';

/**
 * Exchange rate service.
 *
 * Responsibilities:
 * - Calls the backend exchange rates endpoint through ApiService.
 * - Exposes a typed Observable<ExchangeRateResponse>.
 * - Contains no component-specific state or DOM/UI logic.
 *
 * Endpoint: GET /api/exchange-rates
 */
@Injectable({ providedIn: 'root' })
export class ExchangeRateService {
  private readonly api = inject(ApiService);

  /**
   * Fetches current NBP Table A exchange rates from the backend.
   * The path is relative; ApiService prepends the configured base URL.
   */
  getExchangeRates(): Observable<ExchangeRateResponse> {
    return this.api.get<ExchangeRateResponse>('/exchange-rates');
  }
}
