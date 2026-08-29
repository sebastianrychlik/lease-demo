import { ChangeDetectionStrategy, Component, forwardRef, input } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { ErrorStateMatcher } from '@angular/material/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';

/** A single `app-select` option. */
export interface AppSelectOption<T> {
  value: T;
  label: string;
}

/**
 * Shows the error state whenever the host component's `invalid` input is
 * true — mirrors `app-input`'s error-state convention (see there for the
 * rationale).
 */
class AppSelectErrorStateMatcher implements ErrorStateMatcher {
  constructor(private readonly isInvalid: () => boolean) {}

  isErrorState(): boolean {
    return this.isInvalid();
  }
}

/**
 * LeaseDemo application-level select/dropdown.
 *
 * Wraps Angular Material's form field + select primitives with a small
 * LeaseDemo API and standard `ControlValueAccessor` integration, exactly
 * like `app-input`. Options are supplied as a typed `AppSelectOption<T>[]`
 * so callers never need to touch `mat-select`/`mat-option` directly.
 *
 * Usage:
 * ```html
 * <app-select
 *   label="Market"
 *   [options]="[{ value: 'PL', label: 'PL' }, { value: 'DE', label: 'DE' }]"
 *   [formControl]="marketControl"
 * />
 * ```
 */
@Component({
  selector: 'app-select',
  standalone: true,
  imports: [MatFormFieldModule, MatSelectModule],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SelectComponent),
      multi: true,
    },
  ],
  template: `
    <mat-form-field class="app-select" appearance="outline" [class.app-select--invalid]="invalid()">
      @if (label()) {
        <mat-label>{{ label() }}</mat-label>
      }
      <mat-select
        [value]="value"
        [disabled]="disabled()"
        [errorStateMatcher]="errorStateMatcher"
        (selectionChange)="onSelectionChange($event.value)"
        (blur)="onTouched()"
      >
        @for (option of options(); track option.value) {
          <mat-option [value]="option.value">{{ option.label }}</mat-option>
        }
      </mat-select>
      @if (invalid() && errorMessage()) {
        <mat-error>{{ errorMessage() }}</mat-error>
      }
    </mat-form-field>
  `,
  styleUrl: './select.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SelectComponent<T = unknown> implements ControlValueAccessor {
  readonly label = input<string | undefined>(undefined);
  readonly options = input.required<AppSelectOption<T>[]>();
  readonly invalid = input<boolean>(false);
  readonly errorMessage = input<string | undefined>(undefined);
  readonly disabledInput = input<boolean>(false, { alias: 'disabled' });

  value: T | null = null;
  disabledState = false;

  readonly errorStateMatcher = new AppSelectErrorStateMatcher(() => this.invalid());

  private onChange: (value: T | null) => void = () => {};
  onTouched: () => void = () => {};

  disabled(): boolean {
    return this.disabledState || this.disabledInput();
  }

  onSelectionChange(value: T): void {
    this.value = value;
    this.onChange(value);
  }

  writeValue(value: T | null): void {
    this.value = value;
  }

  registerOnChange(fn: (value: T | null) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabledState = isDisabled;
  }
}
