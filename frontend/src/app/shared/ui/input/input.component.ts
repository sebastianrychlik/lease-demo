import { ChangeDetectionStrategy, Component, forwardRef, input } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { ErrorStateMatcher } from '@angular/material/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

/**
 * Shows the error state whenever the host component's `invalid` input is
 * true, regardless of Angular Forms' own touched/dirty tracking. LeaseDemo
 * callers decide validity externally (e.g. from a parent FormGroup or
 * business-rule validation), so `app-input` must not hide its error purely
 * because the field has not been interacted with yet.
 */
class AppInputErrorStateMatcher implements ErrorStateMatcher {
  constructor(private readonly isInvalid: () => boolean) {}

  isErrorState(): boolean {
    return this.isInvalid();
  }
}

/**
 * LeaseDemo application-level text input.
 *
 * Wraps Angular Material's form field + input primitives (label, focus,
 * accessibility, error-state styling) with a small LeaseDemo API and
 * standard `ControlValueAccessor` integration so it can be used exactly
 * like a native form control with Angular Forms — either template-driven
 * (`[(ngModel)]`) or reactive (`[formControl]` / `formControlName`).
 *
 * This does NOT attempt to replace or reimplement Angular Forms; it only
 * supplies LeaseDemo's visual/label conventions around Material's input.
 *
 * Usage:
 * ```html
 * <app-input label="Email" placeholder="jane.doe@example.com" [(ngModel)]="email" />
 * <app-input label="PESEL" [formControl]="peselControl" errorMessage="Invalid PESEL" />
 * ```
 */
@Component({
  selector: 'app-input',
  standalone: true,
  imports: [FormsModule, MatFormFieldModule, MatInputModule],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => InputComponent),
      multi: true,
    },
  ],
  template: `
    <mat-form-field class="app-input" appearance="outline" [class.app-input--invalid]="invalid()">
      @if (label()) {
        <mat-label>{{ label() }}</mat-label>
      }
      <input
        matInput
        [placeholder]="placeholder() ?? ''"
        [disabled]="disabled()"
        [ngModel]="value"
        [errorStateMatcher]="errorStateMatcher"
        (ngModelChange)="onValueChange($event)"
        (blur)="onTouched()"
      />
      @if (invalid() && errorMessage()) {
        <mat-error>{{ errorMessage() }}</mat-error>
      }
    </mat-form-field>
  `,
  styleUrl: './input.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InputComponent implements ControlValueAccessor {
  /** Optional field label, rendered via Material's floating label. */
  readonly label = input<string | undefined>(undefined);

  /** Optional placeholder text. */
  readonly placeholder = input<string | undefined>(undefined);

  /** Whether the field is currently in an error/invalid state. */
  readonly invalid = input<boolean>(false);

  /** Message shown beneath the field when `invalid()` is true. */
  readonly errorMessage = input<string | undefined>(undefined);

  /**
   * Static disabled flag for non-form usage. Reactive/template-driven forms
   * may also disable this control via the standard `ControlValueAccessor`
   * `setDisabledState` mechanism (e.g. a disabled `FormControl`).
   */
  readonly disabledInput = input<boolean>(false, { alias: 'disabled' });

  value = '';
  disabledState = false;

  readonly errorStateMatcher = new AppInputErrorStateMatcher(() => this.invalid());

  private onChange: (value: string) => void = () => {};
  onTouched: () => void = () => {};

  disabled(): boolean {
    return this.disabledState || this.disabledInput();
  }

  onValueChange(value: string): void {
    this.value = value;
    this.onChange(value);
  }

  writeValue(value: string): void {
    this.value = value ?? '';
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabledState = isDisabled;
  }
}
