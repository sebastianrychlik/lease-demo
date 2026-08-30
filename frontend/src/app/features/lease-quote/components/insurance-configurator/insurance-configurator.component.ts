import { ChangeDetectionStrategy, Component, EventEmitter, Output, ViewChild, input } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatAccordion, MatExpansionModule } from '@angular/material/expansion';
import { TranslocoModule } from '@jsverse/transloco';
import { map, startWith } from 'rxjs';

import { CheckboxComponent } from '../../../../shared/ui';
import { LdCurrencyPipe } from '../../../../shared/pipes/ld-currency.pipe';
import { LeaseCurrency } from '../../models/lease-quote.model';
import {
  INSURANCE_COVERAGE_DEFINITIONS,
  InsuranceConfiguration,
  InsuranceCoverageCode,
  InsuranceCoverageDefinition,
  InsuranceCoverageSelection,
} from '../../models/insurance.model';

/** Typed reactive-form controls for a single coverage row (M5.2 §6, §7). */
export interface InsuranceCoverageFormControls {
  code: FormControl<InsuranceCoverageCode>;
  enabled: FormControl<boolean>;
  option: FormControl<string | null>;
}

/** Typed reactive-form model for the whole Insurance Configurator (M5.2 §6). */
export interface InsuranceFormControls {
  coverages: FormArray<FormGroup<InsuranceCoverageFormControls>>;
}

/**
 * Insurance Configurator — CUSTOMER Lease Quote page (M5.2).
 *
 * Small, self-contained Angular showcase component:
 * - the `coverages` FormArray is generated dynamically from
 *   {@link INSURANCE_COVERAGE_DEFINITIONS} — no hard-coded per-coverage
 *   controls (M5.2 §6);
 * - each coverage is a typed nested `FormGroup` (M5.2 §7) rendered inside a
 *   Material expansion panel (M5.2 §8);
 * - the resulting {@link InsuranceConfiguration} (selected coverages +
 *   total monthly premium) is derived reactively from `form.valueChanges`
 *   (no manual recalculation, no nested subscriptions — M5.2 §13) and
 *   emitted to the parent Lease Quote page via a plain `@Output` (M5.2 §14);
 * - `ChangeDetectionStrategy.OnPush` throughout.
 *
 * This component has NO backend calls and sends nothing to the Lease Quote
 * calculation endpoint — insurance selections are purely client-side state
 * for now (M5.2 §21).
 */
@Component({
  selector: 'app-insurance-configurator',
  standalone: true,
  imports: [ReactiveFormsModule, MatExpansionModule, TranslocoModule, CheckboxComponent, LdCurrencyPipe],
  templateUrl: './insurance-configurator.component.html',
  styleUrl: './insurance-configurator.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InsuranceConfiguratorComponent {
  /**
   * Settlement currency of the currently selected Lease Product (M5.2 §18).
   * Insurance premiums are demo amounts interpreted in this currency —
   * never a hard-coded symbol (M5.2 §19).
   */
  readonly settlementCurrency = input.required<LeaseCurrency>();

  /** Active Angular formatting locale, passed down from the Lease Quote page. */
  readonly locale = input.required<string>();

  /** Static coverage definitions — the sole source of the dynamically generated form (M5.2 §6). */
  readonly definitions: readonly InsuranceCoverageDefinition[] = INSURANCE_COVERAGE_DEFINITIONS;

  /** Emits the current insurance configuration to the parent Lease Quote page (M5.2 §14). */
  @Output() readonly configurationChange = new EventEmitter<InsuranceConfiguration>();

  /**
   * @ViewChild is used HERE — and ONLY here — for a purely imperative,
   * UI-only purpose: driving the Material accordion's expand/collapse-all
   * behavior (M5.2 §15/§16). It never carries business data; the insurance
   * selection and premium calculation flow entirely through the typed
   * Reactive Form below and are emitted via `@Output`. This distinction
   * (ViewChild for imperative UI vs. Reactive Forms for business data) is a
   * deliberate interview/demo talking point.
   */
  @ViewChild(MatAccordion) private accordion?: MatAccordion;

  /** Dynamically built FormArray — one FormGroup per coverage definition (M5.2 §6). */
  readonly form = new FormGroup<InsuranceFormControls>({
    coverages: new FormArray(INSURANCE_COVERAGE_DEFINITIONS.map((definition) => buildCoverageGroup(definition))),
  });

  /** Reactive premium/selection stream — recomputed on every form change, no manual recalculation (M5.2 §13). */
  readonly configuration$ = this.form.controls.coverages.valueChanges.pipe(
    startWith(this.form.controls.coverages.value),
    map(() => computeConfiguration(this.form.controls.coverages.getRawValue())),
  );

  /** Signal view of {@link configuration$} for the template (§ premium/summary display). */
  readonly configuration = toSignal(this.configuration$, {
    initialValue: computeConfiguration(this.form.controls.coverages.getRawValue()),
  });

  constructor() {
    // Single subscription owned by this component (no nested subscriptions)
    // re-emits the derived configuration to the parent on every change.
    this.configuration$.subscribe((configuration) => this.configurationChange.emit(configuration));

    // Lightweight validation (M5.2 §26): `option` is required only while a
    // coverage with options is enabled; disabling it clears the requirement
    // so a hidden option never blocks form validity.
    this.form.controls.coverages.controls.forEach((group) => {
      const hasOptions = !!this.definitionFor(group.controls.code.value).options?.length;
      if (!hasOptions) {
        return;
      }
      group.controls.enabled.valueChanges
        .pipe(startWith(group.controls.enabled.value))
        .subscribe((enabled) => {
          group.controls.option.setValidators(enabled ? [Validators.required] : []);
          group.controls.option.updateValueAndValidity({ emitEvent: false });
        });
    });
  }

  coverageGroups(): FormGroup<InsuranceCoverageFormControls>[] {
    return this.form.controls.coverages.controls;
  }

  definitionFor(code: InsuranceCoverageCode): InsuranceCoverageDefinition {
    return this.definitions.find((definition) => definition.code === code)!;
  }

  /** Imperative, UI-only accordion action — see the ViewChild comment above. */
  expandAll(): void {
    this.accordion?.openAll();
  }

  /** Imperative, UI-only accordion action — see the ViewChild comment above. */
  collapseAll(): void {
    this.accordion?.closeAll();
  }
}

function buildCoverageGroup(definition: InsuranceCoverageDefinition): FormGroup<InsuranceCoverageFormControls> {
  return new FormGroup<InsuranceCoverageFormControls>({
    code: new FormControl(definition.code, { nonNullable: true }),
    enabled: new FormControl(false, { nonNullable: true }),
    option: new FormControl<string | null>(null),
  });
}

/**
 * Pure premium/selection computation (M5.2 §12, §35 — the same logic is
 * exercised directly by unit tests without needing a full TestBed).
 *
 * A disabled coverage never contributes to the total, even if a stale
 * `option` value remains on the control (M5.2 §9/§26).
 */
export function computeConfiguration(
  rows: { code: InsuranceCoverageCode; enabled: boolean; option: string | null }[],
): InsuranceConfiguration {
  const coverages: InsuranceCoverageSelection[] = rows.map((row) => {
    const definition = INSURANCE_COVERAGE_DEFINITIONS.find((candidate) => candidate.code === row.code)!;

    if (!row.enabled) {
      return { code: row.code, enabled: false, option: null, monthlyPremium: 0 };
    }
    if (definition.options?.length) {
      const selectedOption = definition.options.find((option) => option.value === row.option);
      return {
        code: row.code,
        enabled: true,
        option: selectedOption?.value ?? null,
        monthlyPremium: selectedOption?.monthlyPremium ?? 0,
      };
    }
    return { code: row.code, enabled: true, option: null, monthlyPremium: definition.monthlyPremium };
  });

  const totalMonthlyPremium = coverages.reduce((sum, coverage) => sum + coverage.monthlyPremium, 0);

  return { coverages, totalMonthlyPremium };
}
