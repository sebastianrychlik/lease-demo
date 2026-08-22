import {
  ChangeDetectionStrategy,
  Component,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';

import { ExchangeRateService } from '../services/exchange-rate.service';
import { ExchangeRateResponse } from '../models/exchange-rate.model';

/** Discriminated union representing all possible UI states. */
type ExchangeRatesViewState =
  | { status: 'loading' }
  | { status: 'success'; data: ExchangeRateResponse }
  | { status: 'empty' }
  | { status: 'error'; message: string };

/**
 * Exchange Rates page component.
 *
 * Displays current NBP Table A exchange rates retrieved from GET /api/exchange-rates.
 * Models loading, success, empty, and error states explicitly using Angular Signals.
 */
@Component({
  selector: 'app-exchange-rates-page',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="exchange-rates">
      <h2 class="exchange-rates__title">Exchange Rates</h2>

      @if (viewState().status === 'loading') {
        <p class="exchange-rates__state exchange-rates__state--loading">
          Loading...
        </p>
      }

      @if (viewState().status === 'success') {
        <p class="exchange-rates__meta">
          Table: {{ asSuccess(viewState()).data.tableNo }} &mdash;
          Date: {{ asSuccess(viewState()).data.effectiveDate }}
        </p>
        <table class="exchange-rates__table">
          <thead>
            <tr>
              <th class="exchange-rates__th">Code</th>
              <th class="exchange-rates__th">Currency</th>
              <th class="exchange-rates__th exchange-rates__th--rate">Mid Rate (PLN)</th>
            </tr>
          </thead>
          <tbody>
            @for (rate of asSuccess(viewState()).data.rates; track rate.code) {
              <tr class="exchange-rates__row">
                <td class="exchange-rates__td exchange-rates__td--code">{{ rate.code }}</td>
                <td class="exchange-rates__td">{{ rate.name }}</td>
                <td class="exchange-rates__td exchange-rates__td--rate">{{ rate.midRate | number:'1.4-4' }}</td>
              </tr>
            }
          </tbody>
        </table>
      }

      @if (viewState().status === 'empty') {
        <p class="exchange-rates__state exchange-rates__state--empty">
          No exchange rates available.
        </p>
      }

      @if (viewState().status === 'error') {
        <p class="exchange-rates__state exchange-rates__state--error">
          Error downloading data
        </p>
        <p class="exchange-rates__error-message">
          {{ asError(viewState()).message }}
        </p>
        <button class="exchange-rates__retry" (click)="load()">Retry</button>
      }
    </section>
  `,
  styles: [
    `
      .exchange-rates {
        display: flex;
        flex-direction: column;
        align-items: center;
        padding: 2rem 1rem;
        gap: 1rem;
      }

      .exchange-rates__title {
        font-size: 1.5rem;
        margin-bottom: 0.5rem;
      }

      .exchange-rates__meta {
        font-size: 0.9rem;
        color: #616161;
      }

      .exchange-rates__state {
        font-size: 1.1rem;
        font-weight: 500;
        margin-top: 2rem;
      }

      .exchange-rates__state--loading {
        color: #757575;
      }

      .exchange-rates__state--error {
        color: #c62828;
      }

      .exchange-rates__state--empty {
        color: #757575;
      }

      .exchange-rates__error-message {
        color: #757575;
        font-size: 0.9rem;
        max-width: 30rem;
        text-align: center;
      }

      .exchange-rates__retry {
        margin-top: 0.5rem;
        padding: 0.5rem 1.5rem;
        font-size: 1rem;
        cursor: pointer;
        border: 1px solid #1565c0;
        border-radius: 4px;
        background: #fff;
        color: #1565c0;
      }

      .exchange-rates__retry:hover {
        background: #e3f2fd;
      }

      .exchange-rates__table {
        width: 100%;
        max-width: 720px;
        border-collapse: collapse;
        font-size: 0.95rem;
      }

      .exchange-rates__th {
        text-align: left;
        padding: 0.6rem 1rem;
        background: #f5f5f5;
        border-bottom: 2px solid #e0e0e0;
        font-weight: 600;
      }

      .exchange-rates__th--rate,
      .exchange-rates__td--rate {
        text-align: right;
      }

      .exchange-rates__td {
        padding: 0.5rem 1rem;
        border-bottom: 1px solid #eeeeee;
      }

      .exchange-rates__td--code {
        font-weight: 600;
        color: #1565c0;
      }

      .exchange-rates__row:hover {
        background: #fafafa;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExchangeRatesPageComponent implements OnInit {
  private readonly exchangeRateService = inject(ExchangeRateService);

  readonly viewState = signal<ExchangeRatesViewState>({ status: 'loading' });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.viewState.set({ status: 'loading' });
    this.exchangeRateService.getExchangeRates().subscribe({
      next: (data) => {
        if (!data.rates || data.rates.length === 0) {
          this.viewState.set({ status: 'empty' });
        } else {
          this.viewState.set({ status: 'success', data });
        }
      },
      error: (err: unknown) => {
        const message =
          err instanceof Error ? err.message : 'Unable to reach the backend.';
        this.viewState.set({ status: 'error', message });
      },
    });
  }

  /** Narrows a state to the success variant for template access. */
  asSuccess(
    state: ExchangeRatesViewState
  ): Extract<ExchangeRatesViewState, { status: 'success' }> {
    return state as Extract<ExchangeRatesViewState, { status: 'success' }>;
  }

  /** Narrows a state to the error variant for template access. */
  asError(
    state: ExchangeRatesViewState
  ): Extract<ExchangeRatesViewState, { status: 'error' }> {
    return state as Extract<ExchangeRatesViewState, { status: 'error' }>;
  }
}
