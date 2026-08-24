import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiService } from '../../../../core/services/api.service';
import { CustomerCreateRequest, CustomerProfile } from '../models/customer-profile.model';

/**
 * Discriminated union describing the current Customer profile state.
 *
 * - `loading`: initial state, or a fetch is in flight.
 * - `found`: the authenticated identity has a persisted Customer profile.
 * - `missing`: the authenticated identity has NOT completed onboarding yet
 *   (backend returned 404 for GET /api/customers/me) — an expected state,
 *   not an error.
 * - `error`: any other failure (network, 5xx, etc).
 */
export type CurrentCustomerState =
  | { status: 'loading' }
  | { status: 'found'; profile: CustomerProfile }
  | { status: 'missing' }
  | { status: 'error'; message: string };

/**
 * Single source of truth for "does the authenticated CUSTOMER have a
 * persisted domain profile yet?".
 *
 * This is the ONE place in the frontend that calls
 * `GET /api/customers/me` — route guards, the dashboard, and the profile
 * page all read from {@link state} / {@link profile} rather than issuing
 * their own requests. `providedIn: 'root'` gives it a single instance for
 * the whole application, so the profile is fetched once and shared.
 *
 * IMPORTANT: This service does not decide *which* Customer identity is
 * loaded — the backend derives that from the authenticated JWT subject.
 * The frontend never supplies a customerId/keycloakUserId/email here.
 */
@Injectable({ providedIn: 'root' })
export class CurrentCustomerService {
  private readonly api = inject(ApiService);

  private readonly _state = signal<CurrentCustomerState>({ status: 'loading' });

  /** Reactive read-only current-profile state. */
  readonly state = this._state.asReadonly();

  /** Convenience: the loaded profile, or `null` when not in the `found` state. */
  readonly profile = computed<CustomerProfile | null>(() => {
    const state = this._state();
    return state.status === 'found' ? state.profile : null;
  });

  /**
   * Loads (or reloads) the current Customer profile from the backend.
   *
   * Safe to call multiple times (e.g. after a route re-entry) — each call
   * re-issues the request and replaces the cached state. Callers that only
   * need to read the cached state should use {@link state} / {@link profile}
   * instead of calling this repeatedly.
   */
  async load(): Promise<void> {
    this._state.set({ status: 'loading' });
    try {
      const profile = await firstValueFrom(this.api.get<CustomerProfile>('/customers/me'));
      this._state.set({ status: 'found', profile });
    } catch (error) {
      if (error instanceof HttpErrorResponse && error.status === 404) {
        this._state.set({ status: 'missing' });
        return;
      }
      this._state.set({
        status: 'error',
        message: 'Unable to load your profile right now. Please try again later.',
      });
    }
  }

  /**
   * Submits the onboarding request. The Keycloak identity is never part of
   * this payload — the backend derives it exclusively from the
   * authenticated JWT subject.
   *
   * On success, refreshes the cached profile state so the rest of the
   * application (dashboard, profile page, guards) immediately reflects the
   * newly persisted Customer without a full page reload.
   */
  async createProfile(request: CustomerCreateRequest): Promise<void> {
    const profile = await firstValueFrom(
      this.api.post<CustomerProfile>('/customers', request),
    );
    this._state.set({ status: 'found', profile });
  }
}
