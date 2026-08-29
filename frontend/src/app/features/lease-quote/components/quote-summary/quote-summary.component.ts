import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { LeaseQuoteResponse } from '../../models/lease-quote.model';

/** Discriminated union representing the quote calculation view state. */
export type QuoteSummaryViewState =
  | { status: 'loading' }
  | { status: 'success'; quote: LeaseQuoteResponse }
  | { status: 'error'; message: string };

/**
 * Presentational quote summary panel.
 *
 * Purely renders a {@link QuoteSummaryViewState} produced by the page
 * component's RxJS pipeline. Contains no HTTP calls and no calculation
 * logic — the backend is the sole calculation authority (M5.1).
 */
@Component({
  selector: 'app-quote-summary',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './quote-summary.component.html',
  styleUrl: './quote-summary.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuoteSummaryComponent {
  readonly viewState = input.required<QuoteSummaryViewState>();
}
