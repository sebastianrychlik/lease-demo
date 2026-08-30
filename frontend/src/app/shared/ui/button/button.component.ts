import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';

/** Supported LeaseDemo button visual variants. */
export type AppButtonVariant = 'primary' | 'secondary' | 'danger';

/** Native `<button>` `type` values `app-button` forwards. */
export type AppButtonType = 'button' | 'submit' | 'reset';

/**
 * LeaseDemo application-level button.
 *
 * Wraps Angular Material's button primitive (real `<button>` semantics,
 * ripple, focus/hover states, accessibility) with a small, stable LeaseDemo
 * API. Features should prefer `<app-button>` over raw `mat-button`/
 * `mat-raised-button` so button styling/behavior stays centralized and
 * consistent across the application.
 *
 * Usage:
 * ```html
 * <app-button variant="primary">Save customer</app-button>
 * <app-button variant="secondary">Cancel</app-button>
 * <app-button variant="danger" [disabled]="true">Delete</app-button>
 * ```
 */
@Component({
  selector: 'app-button',
  standalone: true,
  imports: [MatButtonModule],
  template: `
    <button
      class="app-button"
      [class.app-button--primary]="variant() === 'primary'"
      [class.app-button--secondary]="variant() === 'secondary'"
      [class.app-button--danger]="variant() === 'danger'"
      mat-flat-button
      [type]="type()"
      [disabled]="disabled()"
    >
      <ng-content></ng-content>
    </button>
  `,
  styleUrl: './button.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ButtonComponent {
  /** Visual variant. Defaults to the primary corporate action style. */
  readonly variant = input<AppButtonVariant>('primary');

  /**
   * Native `<button>` `type`, forwarded as-is. Defaults to `'button'` so
   * existing call sites (which never set this) keep their current
   * behavior. Callers that place `<app-button>` inside a `<form>` and want
   * it to trigger `(ngSubmit)` MUST explicitly set `type="submit"` — this
   * component performs no implicit form-submission magic.
   */
  readonly type = input<AppButtonType>('button');

  /** Semantic disabled state — sets the native `disabled` attribute. */
  readonly disabled = input<boolean>(false);
}
