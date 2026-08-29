import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  combineLatest,
  catchError,
  debounceTime,
  distinctUntilChanged,
  map,
  Observable,
  of,
  shareReplay,
  startWith,
  switchMap,
} from 'rxjs';

import { CardComponent, PageHeaderComponent } from '../../../shared/ui';
import { LeaseParametersComponent } from '../components/lease-parameters/lease-parameters.component';
import { QuoteSummaryComponent, QuoteSummaryViewState } from '../components/quote-summary/quote-summary.component';
import {
  LeaseCurrency,
  LeaseQuoteRequest,
  LeaseTermMonths,
  LeaseType,
} from '../models/lease-quote.model';
import { LeaseQuoteService } from '../services/lease-quote.service';

/** Typed reactive-form controls for the Lease Quote Simulator. */
export interface LeaseQuoteFormControls {
  vehiclePrice: FormControl<number>;
  currency: FormControl<LeaseCurrency>;
  termMonths: FormControl<LeaseTermMonths>;
  initialPaymentPercent: FormControl<number>;
  buyoutPercent: FormControl<number>;
  leaseType: FormControl<LeaseType>;
}

/** Debounce window for live recalculation — kept short so the UI feels alive. */
const RECALCULATION_DEBOUNCE_MS = 200;

/**
 * Lease Quote Simulator page — /lease-quote (M5.1).
 *
 * Wires the reactive lease parameters form directly to the backend
 * calculation endpoint with NO "Calculate" button: every relevant control's
 * `valueChanges` is combined, debounced, and mapped to a
 * {@link LeaseQuoteRequest}. `switchMap` guarantees that only the response
 * to the most recent request is ever rendered (stale in-flight requests
 * from rapid slider movement are cancelled). `shareReplay` lets the
 * resulting quote stream be consumed via the `async` pipe without
 * triggering duplicate HTTP calls.
 *
 * The backend (`LeaseQuoteService`) is the sole calculation authority —
 * this component never computes or duplicates the lease formula.
 */
@Component({
  selector: 'app-lease-quote-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    PageHeaderComponent,
    CardComponent,
    LeaseParametersComponent,
    QuoteSummaryComponent,
  ],
  templateUrl: './lease-quote-page.component.html',
  styleUrl: './lease-quote-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LeaseQuotePageComponent {
  private readonly leaseQuoteService = inject(LeaseQuoteService);

  readonly form = new FormGroup<LeaseQuoteFormControls>({
    vehiclePrice: new FormControl(45000, {
      nonNullable: true,
      validators: [Validators.required, Validators.min(1)],
    }),
    currency: new FormControl<LeaseCurrency>('EUR', { nonNullable: true }),
    termMonths: new FormControl<LeaseTermMonths>(36, { nonNullable: true }),
    initialPaymentPercent: new FormControl(20, { nonNullable: true }),
    buyoutPercent: new FormControl(15, { nonNullable: true }),
    leaseType: new FormControl<LeaseType>('OPERATING', { nonNullable: true }),
  });

  private readonly vehiclePrice$ = this.form.controls.vehiclePrice.valueChanges.pipe(
    startWith(this.form.controls.vehiclePrice.value),
    distinctUntilChanged(),
  );

  private readonly currency$ = this.form.controls.currency.valueChanges.pipe(
    startWith(this.form.controls.currency.value),
    distinctUntilChanged(),
  );

  private readonly termMonths$ = this.form.controls.termMonths.valueChanges.pipe(
    startWith(this.form.controls.termMonths.value),
    distinctUntilChanged(),
  );

  private readonly initialPaymentPercent$ = this.form.controls.initialPaymentPercent.valueChanges.pipe(
    startWith(this.form.controls.initialPaymentPercent.value),
    distinctUntilChanged(),
  );

  private readonly buyoutPercent$ = this.form.controls.buyoutPercent.valueChanges.pipe(
    startWith(this.form.controls.buyoutPercent.value),
    distinctUntilChanged(),
  );

  private readonly leaseType$ = this.form.controls.leaseType.valueChanges.pipe(
    startWith(this.form.controls.leaseType.value),
    distinctUntilChanged(),
  );

  /**
   * Live quote view-state stream: combines every form input, debounces
   * rapid slider/keyboard changes, cancels stale in-flight requests via
   * `switchMap`, and shares the single subscription created by `toSignal`
   * in the template so no manual `.subscribe()` is needed.
   */
  private readonly viewState$: Observable<QuoteSummaryViewState> = combineLatest([
    this.vehiclePrice$,
    this.currency$,
    this.termMonths$,
    this.initialPaymentPercent$,
    this.buyoutPercent$,
    this.leaseType$,
  ]).pipe(
    debounceTime(RECALCULATION_DEBOUNCE_MS),
    map(
      ([vehiclePrice, currency, termMonths, initialPaymentPercent, buyoutPercent, leaseType]): LeaseQuoteRequest => ({
        vehiclePrice,
        currency,
        termMonths,
        initialPaymentPercent,
        buyoutPercent,
        leaseType,
      }),
    ),
    switchMap((request) =>
      this.leaseQuoteService.calculate(request).pipe(
        map((quote): QuoteSummaryViewState => ({ status: 'success', quote })),
        startWith<QuoteSummaryViewState>({ status: 'loading' }),
        catchError((error: unknown) =>
          of<QuoteSummaryViewState>({ status: 'error', message: mapQuoteError(error) }),
        ),
      ),
    ),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  readonly viewState = toSignal(this.viewState$, {
    initialValue: { status: 'loading' } as QuoteSummaryViewState,
  });
}

/** Maps a backend error into a safe, user-facing message. */
function mapQuoteError(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 502) {
      return 'The NBP exchange rate service is currently unavailable. Please try again shortly.';
    }
    if (error.status === 400) {
      return 'Some of the lease parameters are invalid.';
    }
    if (error.status === 401) {
      return 'Your session has expired. Please sign in again.';
    }
  }
  return 'Something went wrong while calculating your quote. Please try again later.';
}
