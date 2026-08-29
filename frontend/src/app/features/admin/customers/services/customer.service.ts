import { inject, Injectable } from '@angular/core';
import { HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApiService } from '../../../../core/services/api.service';
import { CustomerListItem } from '../models/customer-list-item.model';
import { CustomerQuery } from '../models/customer-query.model';
import { PageResponse } from '../models/page-response.model';

/**
 * Admin Customer query service.
 *
 * Responsibilities:
 * - Calls GET /api/customers (ADMIN-only, enforced authoritatively by
 *   Spring Security) through ApiService.
 * - Builds HTTP query parameters from a typed {@link CustomerQuery}.
 * - Exposes a typed Observable<PageResponse<CustomerListItem>>.
 *
 * Authentication is handled transparently by the existing HTTP auth
 * interceptor (see app.config.ts) — this service never attaches tokens
 * itself.
 */
@Injectable({ providedIn: 'root' })
export class CustomerService {
  private readonly api = inject(ApiService);

  getCustomers(query: CustomerQuery): Observable<PageResponse<CustomerListItem>> {
    let params = new HttpParams().set('page', query.page).set('size', query.size);

    if (query.search) {
      params = params.set('search', query.search);
    }
    if (query.sortField) {
      params = params.set('sortField', query.sortField);
    }
    if (query.sortDirection) {
      params = params.set('sortDirection', query.sortDirection);
    }

    return this.api.get<PageResponse<CustomerListItem>>('/customers', params);
  }
}
