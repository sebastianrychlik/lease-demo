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

/** Native `<input>` `type` values `app-input` supports. */
export type AppInputType = 'text' | 'number' | 'date';

/**
 * LeaseDemo application-level text/number/date input.
 *
 * Wraps Angular Material's form field + input primitives (label, focus,
 * accessibility, error-state styling) with a small LeaseDemo API and
 * standard `ControlValueAccessor` integration so it can be used exactly
 * like a native form control with Angular Forms — either template-driven
 * (`[(ngModel)]`) or reactive (`[formControl]` / `formControlName`).
 *
 * `type="number"` binds a numeric `FormControl<number | null>` —
 * `onValueChange` explicitly coerces the native input's string value to a
 * real `number` (or `null` when empty) so callers never need to
 * parse/coerce strings themselves. `type="date"` binds a plain
 * ISO (`YYYY-MM-DD`) string, matching the backend's `LocalDate` JSON
 * shape directly with no extra Date-object conversion.
 *
 * This does NOT attempt to replace or reimplement Angular Forms; it only
 * supplies LeaseDemo's visual/label conventions around Material's input.
 *
 * Usage:
 * ```html
 * <app-input label="Email" placeholder="jane.doe@example.com" [(ngModel)]="email" />
 * <app-input label="PESEL" [formControl]="peselControl" errorMessage="Invalid PESEL" />
 * <app-input type="number" label="Min %" [formControl]="minPercentControl" />
 * <app-input type="date" label="Valid from" [formControl]="validFromControl" />
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
        [type]="type()"
        [step]="step() ?? null"
        [min]="min() ?? null"
        [max]="max() ?? null"
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
  /** Native input type. Defaults to `'text'`. */
  readonly type = input<AppInputType>('text');

  /** Optional field label, rendered via Material's floating label. */
  readonly label = input<string | undefined>(undefined);

  /** Optional placeholder text. */
  readonly placeholder = input<string | undefined>(undefined);

  /** Optional native `step` (relevant for `type="number"`). */
  readonly step = input<number | undefined>(undefined);

  /** Optional native `min` (relevant for `type="number"`). */
  readonly min = input<number | undefined>(undefined);

  /** Optional native `max` (relevant for `type="number"`). */
  readonly max = input<number | undefined>(undefined);

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

  /** Bound value — `string` for `type="text"/"date"`, `number | null` for `type="number"`. */
  value: string | number | null = '';
  disabledState = false;

  readonly errorStateMatcher = new AppInputErrorStateMatcher(() => this.invalid());

  private onChange: (value: string | number | null) => void = () => {};
  onTouched: () => void = () => {};

  disabled(): boolean {
    return this.disabledState || this.disabledInput();
  }

  /**
   * Normalizes the raw value coming off `(ngModelChange)` before handing it
   * to Angular Forms.
   *
   * IMPORTANT: because `[type]="type()"` is a PROPERTY binding rather than a
   * static `type="number"` attribute, Angular's built-in `NumberValueAccessor`
   * (which only activates via a static/text `[attr.type]`-independent
   * selector match at compile time) does NOT attach here — this host always
   * gets the plain `DefaultValueAccessor`/`NgModel` behavior, which emits the
   * native input's raw STRING value regardless of `type="number"`. That was
   * the root cause of validators like `integerRangeValidator`/
   * `Number.isInteger(...)` rejecting perfectly valid numbers such as `51`
   * (received as the string `"51"`).
   *
   * For `type="number"` this explicitly coerces to a real `number`
   * (preserving decimals — no `parseInt`), mapping an empty string to `null`
   * rather than `NaN`/`""`. `text`/`date` values are passed through
   * unchanged.
   */
  onValueChange(value: string | number | null): void {
    const normalized = this.normalize(value);
    this.value = normalized;
    this.onChange(normalized);
  }

  private normalize(value: string | number | null): string | number | null {
    if (this.type() !== 'number') {
      return value;
    }
    if (value === null || value === undefined || value === '') {
      return null;
    }
    const numeric = typeof value === 'number' ? value : Number(value);
    return Number.isNaN(numeric) ? null : numeric;
  }

  writeValue(value: string | number | null): void {
    this.value = value ?? (this.type() === 'number' ? null : '');
  }

  registerOnChange(fn: (value: string | number | null) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabledState = isDisabled;
  }
}
