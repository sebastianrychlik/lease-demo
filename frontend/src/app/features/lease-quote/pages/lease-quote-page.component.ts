import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import {
  combineLatest,
  catchError,
  debounceTime,
  distinctUntilChanged,
  filter,
  map,
  Observable,
  of,
  shareReplay,
  startWith,
  switchMap,
  tap,
} from 'rxjs';

import { LanguageService } from '../../../core/i18n/language.service';
import { AppSelectOption, CardComponent, PageHeaderComponent, SelectComponent } from '../../../shared/ui';
import { LeaseParametersComponent } from '../components/lease-parameters/lease-parameters.component';
import { QuoteSummaryComponent, QuoteSummaryViewState } from '../components/quote-summary/quote-summary.component';
import {
  LeaseCurrency,
  LeaseProductConfiguration,
  LeaseQuoteRequest,
  LeaseType,
  PercentageRangeConfiguration,
} from '../models/lease-quote.model';
import { LeaseProductService } from '../services/lease-product.service';
import { LeaseQuoteService } from '../services/lease-quote.service';

/** Typed reactive-form controls for the Lease Quote Simulator. */
export interface LeaseQuoteFormControls {
  vehiclePrice: FormControl<number>;
  currency: FormControl<LeaseCurrency>;
  termMonths: FormControl<number>;
  initialPaymentPercent: FormControl<number>;
  buyoutPercent: FormControl<number>;
  leaseType: FormControl<LeaseType>;
}

/** Discriminated union representing the Lease Product configuration load state. */
export type ProductConfigViewState =
  | { status: 'loading' }
  | { status: 'empty' }
  | { status: 'error' }
  | { status: 'ready'; product: LeaseProductConfiguration; products: LeaseProductConfiguration[] };

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
    SelectComponent,
    LeaseParametersComponent,
    QuoteSummaryComponent,
    TranslocoModule,
  ],
  templateUrl: './lease-quote-page.component.html',
  styleUrl: './lease-quote-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LeaseQuotePageComponent {
  private readonly leaseProductService = inject(LeaseProductService);
  private readonly leaseQuoteService = inject(LeaseQuoteService);
  private readonly translocoService = inject(TranslocoService);
  private readonly languageService = inject(LanguageService);

  /** Active Angular formatting locale, passed down to `QuoteSummaryComponent` (M5.1.3.1). */
  readonly locale = this.languageService.locale;

  /**
   * Form is constructed eagerly with placeholder values; every value is
   * synchronized to the selected product's defaults/ranges as soon as
   * product configuration arrives (see {@link syncFormToProduct}), so no
   * invalid/hard-coded business value is ever actually submitted.
   */
  readonly form = new FormGroup<LeaseQuoteFormControls>({
    vehiclePrice: new FormControl(45000, {
      nonNullable: true,
      validators: [Validators.required, Validators.min(1)],
    }),
    currency: new FormControl<LeaseCurrency>('EUR', { nonNullable: true }),
    termMonths: new FormControl(36, { nonNullable: true }),
    initialPaymentPercent: new FormControl(20, { nonNullable: true }),
    buyoutPercent: new FormControl(15, { nonNullable: true }),
    leaseType: new FormControl<LeaseType>('OPERATING', { nonNullable: true }),
  });

  /**
   * Explicit CUSTOMER Lease Product selection (M5.1.5) — the single source
   * of truth for "which product is selected". Standalone (not part of
   * `form`) since it drives product-dependent form *configuration* rather
   * than being a quote input itself. `null` until the first product list
   * arrives, at which point the derivation below deterministically selects
   * the first backend-returned product.
   */
  readonly productControl = new FormControl<string | null>(null);

  /**
   * Lease Product configuration load state — loading / empty / error /
   * ready. Combines the shared `availableProducts$` cache with the
   * CUSTOMER's `productControl` selection (no nested subscriptions) to
   * derive the currently selected product by `code` — never by array
   * index. A single subscription (via `shareReplay`) drives the
   * empty/error UI state, the product selector options, and the
   * form-normalization side effect below, so the underlying HTTP call
   * happens exactly once regardless of how many times the product changes.
   */
  private readonly productConfigState$: Observable<ProductConfigViewState> = combineLatest([
    this.leaseProductService.availableProducts$,
    this.productControl.valueChanges.pipe(startWith(this.productControl.value), distinctUntilChanged()),
  ]).pipe(
    map(([products, selectedCode]): ProductConfigViewState => {
      if (products.length === 0) {
        return { status: 'empty' };
      }
      const product = products.find((candidate) => candidate.code === selectedCode) ?? products[0];
      return { status: 'ready', product, products };
    }),
    tap((state) => {
      if (state.status === 'ready') {
        // Reflect the deterministic initial selection (or a code that no
        // longer exists) back onto the control without re-triggering this
        // stream — keeps the selector visibly in sync with actual state.
        if (this.productControl.value !== state.product.code) {
          this.productControl.setValue(state.product.code, { emitEvent: false });
        }
        this.syncFormToProduct(state.product);
      }
    }),
    startWith<ProductConfigViewState>({ status: 'loading' }),
    catchError(() => of<ProductConfigViewState>({ status: 'error' })),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  readonly productConfigState = toSignal(this.productConfigState$, {
    initialValue: { status: 'loading' } as ProductConfigViewState,
  });

  /** Options for the CUSTOMER product selector — `product.code` is the value, `product.name` the label (never Transloco-translated). */
  readonly productOptions = toSignal(
    this.productConfigState$.pipe(
      filter((state): state is { status: 'ready'; product: LeaseProductConfiguration; products: LeaseProductConfiguration[] } => state.status === 'ready'),
      map((state): AppSelectOption<string>[] => state.products.map((product) => ({ value: product.code, label: product.name }))),
      distinctUntilChanged(
        (a, b) => a.length === b.length && a.every((option, index) => option.value === b[index].value && option.label === b[index].label),
      ),
    ),
    { initialValue: [] as AppSelectOption<string>[] },
  );

  /** Only emits once product configuration has successfully loaded — gates the quote request stream. */
  private readonly selectedProduct$ = this.productConfigState$.pipe(
    filter((state): state is { status: 'ready'; product: LeaseProductConfiguration; products: LeaseProductConfiguration[] } => state.status === 'ready'),
    map((state) => state.product),
    distinctUntilChanged((a, b) => a.code === b.code),
  );

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
    this.selectedProduct$,
    this.vehiclePrice$,
    this.currency$,
    this.termMonths$,
    this.initialPaymentPercent$,
    this.buyoutPercent$,
    this.leaseType$,
  ]).pipe(
    debounceTime(RECALCULATION_DEBOUNCE_MS),
    map(
      ([product, vehiclePrice, currency, termMonths, initialPaymentPercent, buyoutPercent, leaseType]): LeaseQuoteRequest => ({
        productCode: product.code,
        vehiclePrice,
        vehiclePriceCurrency: currency,
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
          of<QuoteSummaryViewState>({ status: 'error', message: this.mapQuoteError(error) }),
        ),
      ),
    ),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  readonly viewState = toSignal(this.viewState$, {
    initialValue: { status: 'loading' } as QuoteSummaryViewState,
  });

  /**
   * Ensures the form never carries a value the selected product does not
   * offer (M5.1.2 §24): any control whose current value is not part of
   * the new product's options/range is reset to that product's default.
   */
  private syncFormToProduct(product: LeaseProductConfiguration): void {
    const controls = this.form.controls;

    if (!product.currencies.includes(controls.currency.value)) {
      controls.currency.setValue(product.defaultCurrency);
    }
    if (!product.termsMonths.includes(controls.termMonths.value)) {
      controls.termMonths.setValue(product.defaultTermMonths);
    }
    if (!isWithinRange(controls.initialPaymentPercent.value, product.initialPayment)) {
      controls.initialPaymentPercent.setValue(product.initialPayment.defaultPercent);
    }
    if (!isWithinRange(controls.buyoutPercent.value, product.buyout)) {
      controls.buyoutPercent.setValue(product.buyout.defaultPercent);
    }
    if (!product.leaseTypes.some((leaseType) => leaseType.type === controls.leaseType.value)) {
      controls.leaseType.setValue(product.defaultLeaseType);
    }
  }

  /** Maps a backend error into a safe, translated, user-facing message. */
  private mapQuoteError(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 502) {
        return this.translocoService.translate('leaseQuote.errors.nbpUnavailable');
      }
      if (error.status === 400) {
        return this.translocoService.translate('leaseQuote.errors.invalidParameters');
      }
      if (error.status === 401) {
        return this.translocoService.translate('leaseQuote.errors.sessionExpired');
      }
    }
    return this.translocoService.translate('leaseQuote.errors.generic');
  }
}

function isWithinRange(value: number, range: PercentageRangeConfiguration): boolean {
  return value >= range.minPercent && value <= range.maxPercent;
}
