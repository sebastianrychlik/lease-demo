import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../../core/services/api.service';
import {
  AdminLeaseProduct,
  CreateLeaseProductRequest,
  UpdateLeaseProductRequest,
} from '../models/admin-lease-product.model';

/**
 * ADMIN Lease Product management service (M5.1.4).
 *
 * Calls the dedicated `/api/admin/lease-products` namespace (ADMIN-only,
 * enforced authoritatively by Spring Security) through {@link ApiService}.
 * Contains no business validation of its own — the backend is the sole
 * authority; this service only shapes HTTP calls.
 */
@Injectable({ providedIn: 'root' })
export class AdminLeaseProductService {
  private readonly api = inject(ApiService);

  getAllProducts(): Observable<AdminLeaseProduct[]> {
    return this.api.get<AdminLeaseProduct[]>('/admin/lease-products');
  }

  getProduct(code: string): Observable<AdminLeaseProduct> {
    return this.api.get<AdminLeaseProduct>(`/admin/lease-products/${code}`);
  }

  createProduct(request: CreateLeaseProductRequest): Observable<AdminLeaseProduct> {
    return this.api.post<AdminLeaseProduct>('/admin/lease-products', request);
  }

  updateProduct(code: string, request: UpdateLeaseProductRequest): Observable<AdminLeaseProduct> {
    return this.api.put<AdminLeaseProduct>(`/admin/lease-products/${code}`, request);
  }
}
