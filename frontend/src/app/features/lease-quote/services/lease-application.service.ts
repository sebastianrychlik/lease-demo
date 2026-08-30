import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';
import { CreateLeaseApplicationRequest, LeaseApplicationResponse } from '../models/lease-application.model';

/**
 * Lease application service (M5.3 §33).
 *
 * Calls the backend lease application submission endpoint through
 * ApiService. Contains no calculation/scoring logic — the backend is the
 * sole authority on the recalculated quote, insurance premium, and credit
 * decision.
 *
 * Endpoint: POST /api/lease-applications
 */
@Injectable({ providedIn: 'root' })
export class LeaseApplicationService {
  private readonly api = inject(ApiService);

  submitApplication(request: CreateLeaseApplicationRequest): Observable<LeaseApplicationResponse> {
    return this.api.post<LeaseApplicationResponse>('/lease-applications', request);
  }
}
