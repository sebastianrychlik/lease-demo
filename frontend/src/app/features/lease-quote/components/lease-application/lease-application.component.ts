import { ChangeDetectionStrategy, Component, EventEmitter, Output, inject, input } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';

import { resolveValidationMessage } from '../../../../shared/forms';
import { ButtonComponent, CardComponent, InputComponent } from '../../../../shared/ui';
import { LdCurrencyPipe } from '../../../../shared/pipes/ld-currency.pipe';
import { LeaseApplicationResponse } from '../../models/lease-application.model';

/** Typed reactive-form controls for the M5.3 financial application section. */
export interface LeaseApplicationFormControls {
  monthlyNetIncome: FormControl<number | null>;
  monthlyObligations: FormControl<number | null>;
}

/** Explicit submission state for the Apply action (M5.3 §34) — no NgRx. */
export type LeaseApplicationSubmissionState =
  | { status: 'idle' }
  | { status: 'submitting' }
  | { status: 'success'; result: LeaseApplicationResponse }
  | { status: 'error'; message: string };

/**
 * Lease application section — CUSTOMER Lease Quote page (M5.3 §28–§35).
 *
 * Presentational: owns no HTTP calls. The financial Reactive Form is
 * constructed and owned by the parent page (consistent with
 * `LeaseParametersComponent`); this component only renders it, the Apply
 * button, and the resulting submission state (result card / error).
 */
@Component({
  selector: 'app-lease-application',
  standalone: true,
  imports: [ReactiveFormsModule, CardComponent, InputComponent, ButtonComponent, LdCurrencyPipe, TranslocoModule],
  templateUrl: './lease-application.component.html',
  styleUrl: './lease-application.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LeaseApplicationComponent {
  /** The financial application form, owned by the parent Lease Quote page. */
  readonly formGroup = input.required<FormGroup<LeaseApplicationFormControls>>();

  /** Whether the Apply action is currently allowed (M5.3 §30). */
  readonly canApply = input.required<boolean>();

  /** Current submission state, owned by the parent page. */
  readonly state = input.required<LeaseApplicationSubmissionState>();

  /** Currency to render monetary result values in. */
  readonly settlementCurrency = input.required<string>();

  /** Active Angular formatting locale. */
  readonly locale = input.required<string>();

  /** Emitted when the CUSTOMER clicks Apply. */
  @Output() readonly apply = new EventEmitter<void>();

  private readonly transloco = inject(TranslocoService);

  onApply(): void {
    this.apply.emit();
  }

  /**
   * Translated validation message for a financial field, shown only after
   * the CUSTOMER has interacted with it (M5.5.1) — never before `touched`.
   */
  errorMessage(controlName: keyof LeaseApplicationFormControls): string | undefined {
    const control = this.formGroup().controls[controlName];
    if (!control.touched) {
      return undefined;
    }
    return resolveValidationMessage(this.transloco, control.errors);
  }

  isInvalid(controlName: keyof LeaseApplicationFormControls): boolean {
    const control = this.formGroup().controls[controlName];
    return control.invalid && control.touched;
  }
}
