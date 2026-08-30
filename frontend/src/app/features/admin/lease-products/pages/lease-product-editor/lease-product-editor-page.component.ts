import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { catchError, of } from 'rxjs';

import {
  atLeastOneEnabledValidator,
  dateOrderValidator,
  defaultOutsideRangeValidator,
  duplicateValidator,
  integerRangeValidator,
  maxValueValidator,
  nonNegativeValidator,
  positiveValidator,
  rangeOrderValidator,
  resolveValidationMessage,
  selectedInEnabledGroupValidator,
} from '../../../../../shared/forms';
import {
  AppSelectOption,
  ButtonComponent,
  CardComponent,
  CheckboxComponent,
  InputComponent,
  PageHeaderComponent,
  SelectComponent,
} from '../../../../../shared/ui';
import {
  AdminLeaseProduct,
  CreateLeaseProductRequest,
  LeaseCurrency,
  LeaseType,
  UpdateLeaseProductRequest,
} from '../../models/admin-lease-product.model';
import { AdminLeaseProductService } from '../../services/admin-lease-product.service';

/** Minimum/maximum allowed lease term length in months — mirrors backend `AdminLeaseProductService` validation. */
const MIN_TERM_MONTHS = 6;
const MAX_TERM_MONTHS = 120;

/** APR bounds — mirrors backend `AdminLeaseProductService` validation. */
const MAX_APR_PERCENT = 100;

/** All domain currencies known to the frontend — vocabulary only, ADMIN decides which are enabled per product. */
const ALL_CURRENCIES: LeaseCurrency[] = ['PLN', 'EUR'];

/** All domain lease types known to the frontend — vocabulary only, ADMIN decides which are enabled per product. */
const ALL_LEASE_TYPES: LeaseType[] = ['OPERATING', 'FINANCIAL'];

type EditorViewState =
  | { status: 'loading' }
  | { status: 'ready' }
  | { status: 'saving' }
  | { status: 'error'; message: string };

/**
 * Shared ADMIN Lease Product create/edit page (M5.1.4).
 *
 * `/admin/lease-products/new` and `/admin/lease-products/:code/edit` both
 * route here — `isEditMode` (derived from the presence of a `:code` route
 * param) decides whether the form loads existing data and whether the
 * code field is editable. Uses Angular Reactive Forms exclusively.
 */
@Component({
  selector: 'app-lease-product-editor-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    PageHeaderComponent,
    CardComponent,
    InputComponent,
    SelectComponent,
    CheckboxComponent,
    ButtonComponent,
    TranslocoModule,
  ],
  templateUrl: './lease-product-editor-page.component.html',
  styleUrl: './lease-product-editor-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LeaseProductEditorPageComponent implements OnInit {
  private readonly adminLeaseProductService = inject(AdminLeaseProductService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly translocoService = inject(TranslocoService);

  readonly allCurrencies = ALL_CURRENCIES;
  readonly allLeaseTypes = ALL_LEASE_TYPES;

  readonly marketOptions: AppSelectOption<'PL' | 'DE'>[] = [
    { value: 'PL', label: 'PL' },
    { value: 'DE', label: 'DE' },
  ];

  readonly currencyOptions: AppSelectOption<LeaseCurrency>[] = ALL_CURRENCIES.map((c) => ({
    value: c,
    label: c,
  }));

  readonly leaseTypeOptions = computed<AppSelectOption<LeaseType>[]>(() =>
    ALL_LEASE_TYPES.map((t) => ({
      value: t,
      label: this.translocoService.translate(`leaseQuote.leaseType.${t.toLowerCase()}`),
    })),
  );

  termOptions(): AppSelectOption<number>[] {
    return this.form.controls.terms.value.map((term) => ({ value: term, label: String(term) }));
  }

  readonly code = signal<string | null>(null);
  readonly isEditMode = computed(() => this.code() !== null);
  readonly viewState = signal<EditorViewState>({ status: 'loading' });
  readonly newTermValue = new FormControl<number | null>(null, {
    validators: [integerRangeValidator(MIN_TERM_MONTHS, MAX_TERM_MONTHS), duplicateValidator(() => this.form.controls.terms.value)],
  });

  readonly form = new FormGroup(
    {
      code: new FormControl('', {
        nonNullable: true,
        validators: [Validators.required, Validators.pattern(/^[A-Z0-9_]+$/)],
      }),
      name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
      market: new FormControl<'PL' | 'DE'>('PL', { nonNullable: true, validators: [Validators.required] }),
      enabled: new FormControl(true, { nonNullable: true }),
      validFrom: new FormControl<string | null>(null),
      validTo: new FormControl<string | null>(null),

      currencies: new FormGroup(
        Object.fromEntries(ALL_CURRENCIES.map((c) => [c, new FormControl(false, { nonNullable: true })])),
      ),
      settlementCurrency: new FormControl<LeaseCurrency>('PLN', {
        nonNullable: true,
        validators: [Validators.required],
      }),
      defaultCurrency: new FormControl<LeaseCurrency>('PLN', { nonNullable: true, validators: [Validators.required] }),

      terms: new FormControl<number[]>([], { nonNullable: true, validators: [Validators.required] }),
      defaultTermMonths: new FormControl<number | null>(null, { validators: [Validators.required] }),

      initialPaymentMin: new FormControl(0, { nonNullable: true, validators: [Validators.required, nonNegativeValidator()] }),
      initialPaymentMax: new FormControl(0, { nonNullable: true, validators: [Validators.required, nonNegativeValidator()] }),
      initialPaymentDefault: new FormControl(0, {
        nonNullable: true,
        validators: [Validators.required, nonNegativeValidator()],
      }),
      initialPaymentStep: new FormControl(1, { nonNullable: true, validators: [Validators.required, positiveValidator()] }),

      buyoutMin: new FormControl(0, { nonNullable: true, validators: [Validators.required, nonNegativeValidator()] }),
      buyoutMax: new FormControl(0, { nonNullable: true, validators: [Validators.required, nonNegativeValidator()] }),
      buyoutDefault: new FormControl(0, { nonNullable: true, validators: [Validators.required, nonNegativeValidator()] }),
      buyoutStep: new FormControl(1, { nonNullable: true, validators: [Validators.required, positiveValidator()] }),

      leaseTypesEnabled: new FormGroup(
        Object.fromEntries(ALL_LEASE_TYPES.map((t) => [t, new FormControl(false, { nonNullable: true })])),
      ),
      leaseTypeRates: new FormGroup(
        Object.fromEntries(
          ALL_LEASE_TYPES.map((t) => [
            t,
            new FormControl<number | null>(null, { validators: [positiveValidator(), maxValueValidator(MAX_APR_PERCENT)] }),
          ]),
        ),
      ),
      defaultLeaseType: new FormControl<LeaseType>('OPERATING', {
        nonNullable: true,
        validators: [Validators.required],
      }),
    },
    {
      validators: [
        rangeOrderValidator('initialPaymentMin', 'initialPaymentMax'),
        defaultOutsideRangeValidator('initialPaymentMin', 'initialPaymentMax', 'initialPaymentDefault'),
        rangeOrderValidator('buyoutMin', 'buyoutMax'),
        defaultOutsideRangeValidator('buyoutMin', 'buyoutMax', 'buyoutDefault'),
        dateOrderValidator('validFrom', 'validTo'),
        atLeastOneEnabledValidator('currencies'),
        selectedInEnabledGroupValidator('currencies', 'settlementCurrency'),
        selectedInEnabledGroupValidator('currencies', 'defaultCurrency'),
        atLeastOneEnabledValidator('leaseTypesEnabled'),
        selectedInEnabledGroupValidator('leaseTypesEnabled', 'defaultLeaseType'),
      ],
    },
  );

  /** Resolves a control's current validation error (if any) to a translated message — see `shared/forms`. */
  errorMessage(control: AbstractControl | null | undefined): string | undefined {
    return resolveValidationMessage(this.translocoService, control?.errors);
  }

  /**
   * `duplicateValidator(() => this.form.controls.terms.value)` reads
   * `terms` as an EXTERNAL dependency — Angular has no way of knowing that
   * `newTermValue` must be re-validated whenever `terms` changes (add/
   * remove/load). Without this explicit call, a term typed as a duplicate
   * would keep showing `duplicate` even after the conflicting term is
   * removed, until the user edits the input again.
   */
  private revalidateNewTerm(): void {
    this.newTermValue.updateValueAndValidity({ emitEvent: false });
  }

  ngOnInit(): void {
    const code = this.route.snapshot.paramMap.get('code');
    if (code) {
      this.code.set(code);
      this.form.controls.code.disable();
      this.loadProduct(code);
    } else {
      this.viewState.set({ status: 'ready' });
    }

    // APR validation applies only to configured/enabled lease types (M5.1.4 §14):
    // toggle `required` on each rate control as its "enabled" checkbox changes.
    for (const leaseType of ALL_LEASE_TYPES) {
      const enabledControl = this.form.controls.leaseTypesEnabled.controls[leaseType];
      const rateControl = this.form.controls.leaseTypeRates.controls[leaseType];
      const syncRequired = (enabled: boolean) => {
        rateControl.setValidators(
          enabled
            ? [Validators.required, positiveValidator(), maxValueValidator(MAX_APR_PERCENT)]
            : [positiveValidator(), maxValueValidator(MAX_APR_PERCENT)],
        );
        rateControl.updateValueAndValidity({ emitEvent: false });
      };
      syncRequired(enabledControl.value);
      enabledControl.valueChanges.subscribe(syncRequired);
    }
  }

  private loadProduct(code: string): void {
    this.viewState.set({ status: 'loading' });
    this.adminLeaseProductService
      .getProduct(code)
      .pipe(
        catchError(() => {
          this.viewState.set({
            status: 'error',
            message: this.translocoService.translate('admin.leaseProducts.errors.loadFailed'),
          });
          return of(null);
        }),
      )
      .subscribe((product) => {
        if (!product) {
          return;
        }
        this.populateForm(product);
        this.viewState.set({ status: 'ready' });
      });
  }

  private populateForm(product: AdminLeaseProduct): void {
    this.form.patchValue({
      code: product.code,
      name: product.name,
      market: product.market as 'PL' | 'DE',
      enabled: product.enabled,
      validFrom: product.validFrom,
      validTo: product.validTo,
      settlementCurrency: product.settlementCurrency,
      defaultCurrency: product.defaultCurrency,
      terms: product.termsMonths,
      defaultTermMonths: product.defaultTermMonths,
      initialPaymentMin: product.initialPayment.minPercent,
      initialPaymentMax: product.initialPayment.maxPercent,
      initialPaymentDefault: product.initialPayment.defaultPercent,
      initialPaymentStep: product.initialPayment.stepPercent,
      buyoutMin: product.buyout.minPercent,
      buyoutMax: product.buyout.maxPercent,
      buyoutDefault: product.buyout.defaultPercent,
      buyoutStep: product.buyout.stepPercent,
      defaultLeaseType: product.defaultLeaseType,
    });

    for (const currency of ALL_CURRENCIES) {
      this.form.controls.currencies.controls[currency].setValue(product.currencies.includes(currency));
    }
    for (const leaseType of ALL_LEASE_TYPES) {
      const option = product.leaseTypes.find((lt) => lt.type === leaseType);
      this.form.controls.leaseTypesEnabled.controls[leaseType].setValue(!!option);
      this.form.controls.leaseTypeRates.controls[leaseType].setValue(option ? option.annualRatePercent : null);
    }

    this.revalidateNewTerm();
  }

  addTerm(): void {
    this.newTermValue.markAsTouched();
    this.newTermValue.updateValueAndValidity();
    if (this.newTermValue.invalid) {
      // Invalid/duplicate terms (e.g. -4, 24.5) must never reach `form.controls.terms` (M5.1.4 §10).
      return;
    }
    const value = this.newTermValue.value;
    if (value === null || value === undefined) {
      return;
    }
    const current = this.form.controls.terms.value;
    this.form.controls.terms.setValue([...current, value].sort((a, b) => a - b));
    this.newTermValue.reset(null);
    this.revalidateNewTerm();
  }

  removeTerm(term: number): void {
    const remaining = this.form.controls.terms.value.filter((t) => t !== term);
    this.form.controls.terms.setValue(remaining);

    // Explicit, predictable default-term handling (M5.1.4 §11): if the
    // removed term was the configured default, fall back to another
    // configured term rather than silently keeping/submitting a stale value.
    if (this.form.controls.defaultTermMonths.value === term) {
      this.form.controls.defaultTermMonths.setValue(remaining[0] ?? null);
    }

    // A value the user is currently typing may have been flagged as a
    // `duplicate` against the just-removed term — it must become valid
    // immediately, without requiring the user to touch the input again.
    this.revalidateNewTerm();
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const selectedCurrencies = ALL_CURRENCIES.filter((c) => raw.currencies[c]);
    const selectedLeaseTypes = ALL_LEASE_TYPES.filter((t) => raw.leaseTypesEnabled[t]).map((t) => ({
      type: t,
      annualRatePercent: raw.leaseTypeRates[t] ?? 0,
    }));

    const payload: UpdateLeaseProductRequest = {
      name: raw.name,
      market: raw.market,
      enabled: raw.enabled,
      validFrom: raw.validFrom,
      validTo: raw.validTo,
      currencies: selectedCurrencies,
      settlementCurrency: raw.settlementCurrency,
      defaultCurrency: raw.defaultCurrency,
      terms: raw.terms,
      defaultTermMonths: raw.defaultTermMonths!,
      initialPayment: {
        minPercent: raw.initialPaymentMin,
        maxPercent: raw.initialPaymentMax,
        defaultPercent: raw.initialPaymentDefault,
        stepPercent: raw.initialPaymentStep,
      },
      buyout: {
        minPercent: raw.buyoutMin,
        maxPercent: raw.buyoutMax,
        defaultPercent: raw.buyoutDefault,
        stepPercent: raw.buyoutStep,
      },
      leaseTypes: selectedLeaseTypes,
      defaultLeaseType: raw.defaultLeaseType,
    };

    this.viewState.set({ status: 'saving' });

    const request$ = this.isEditMode()
      ? this.adminLeaseProductService.updateProduct(this.code()!, payload)
      : this.adminLeaseProductService.createProduct({ ...payload, code: raw.code } as CreateLeaseProductRequest);

    request$
      .pipe(
        catchError((error: unknown) => {
          this.viewState.set({ status: 'error', message: this.mapSaveError(error) });
          return of(null);
        }),
      )
      .subscribe((result) => {
        if (!result) {
          return;
        }
        this.router.navigate(['/admin/lease-products']);
      });
  }

  cancel(): void {
    this.router.navigate(['/admin/lease-products']);
  }

  private mapSaveError(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.error?.detail) {
      return error.error.detail as string;
    }
    return this.translocoService.translate('admin.leaseProducts.errors.saveFailed');
  }
}
