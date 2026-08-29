import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * LeaseDemo application-level page header.
 *
 * Establishes the standard page-top pattern: a semantic `<h1>` title, an
 * optional subtitle, and an optional projected actions area (typically one
 * or more `<app-button>`s) aligned to the trailing edge.
 *
 * Usage:
 * ```html
 * <app-page-header title="Customers" subtitle="Manage leasing customers">
 *   <app-button>Add customer</app-button>
 * </app-page-header>
 * ```
 */
@Component({
  selector: 'app-page-header',
  standalone: true,
  template: `
    <header class="app-page-header">
      <div class="app-page-header__text">
        <h1 class="app-page-header__title">{{ title() }}</h1>
        @if (subtitle()) {
          <p class="app-page-header__subtitle">{{ subtitle() }}</p>
        }
      </div>
      <div class="app-page-header__actions">
        <ng-content></ng-content>
      </div>
    </header>
  `,
  styleUrl: './page-header.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PageHeaderComponent {
  /** Required page title, rendered as a semantic `<h1>`. */
  readonly title = input.required<string>();

  /** Optional page subtitle/description. */
  readonly subtitle = input<string | undefined>(undefined);
}
