import { ChangeDetectionStrategy, Component, forwardRef, input } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MatCheckboxModule } from '@angular/material/checkbox';

/**
 * LeaseDemo application-level checkbox.
 *
 * Wraps Angular Material's checkbox primitive with standard
 * `ControlValueAccessor` integration, exactly like `app-input`/`app-select`
 * — so features never reach for a raw `<input type="checkbox">` or
 * `mat-checkbox` directly.
 *
 * Usage:
 * ```html
 * <app-checkbox label="Enabled" [formControl]="enabledControl" />
 * ```
 */
@Component({
  selector: 'app-checkbox',
  standalone: true,
  imports: [MatCheckboxModule],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CheckboxComponent),
      multi: true,
    },
  ],
  template: `
    <mat-checkbox
      class="app-checkbox"
      [checked]="value"
      [disabled]="disabled()"
      (change)="onCheckedChange($event.checked)"
      (blur)="onTouched()"
    >
      {{ label() }}
    </mat-checkbox>
  `,
  styleUrl: './checkbox.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CheckboxComponent implements ControlValueAccessor {
  readonly label = input<string | undefined>(undefined);
  readonly disabledInput = input<boolean>(false, { alias: 'disabled' });

  value = false;
  disabledState = false;

  private onChange: (value: boolean) => void = () => {};
  onTouched: () => void = () => {};

  disabled(): boolean {
    return this.disabledState || this.disabledInput();
  }

  onCheckedChange(checked: boolean): void {
    this.value = checked;
    this.onChange(checked);
  }

  writeValue(value: boolean): void {
    this.value = !!value;
  }

  registerOnChange(fn: (value: boolean) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabledState = isDisabled;
  }
}
