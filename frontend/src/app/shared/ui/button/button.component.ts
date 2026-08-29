import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';

/** Supported LeaseDemo button visual variants. */
export type AppButtonVariant = 'primary' | 'secondary' | 'danger';

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
      type="button"
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

  /** Semantic disabled state — sets the native `disabled` attribute. */
  readonly disabled = input<boolean>(false);
}
