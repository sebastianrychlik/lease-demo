import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { CardComponent, PageHeaderComponent } from '../../ui';

/**
 * Generic feature-placeholder page.
 *
 * Used by routes such as /admin/customers, /admin/leases, /customer/leases,
 * /customer/documents, /customer/profile — these are NOT implemented in
 * this milestone. This component only communicates that clearly; it must
 * never grow CRUD/forms/table functionality itself.
 */
@Component({
  selector: 'app-feature-placeholder',
  standalone: true,
  imports: [PageHeaderComponent, CardComponent],
  template: `
    <app-page-header [title]="title()"></app-page-header>
    <app-card>
      <p>{{ description() }}</p>
    </app-card>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FeaturePlaceholderComponent {
  readonly title = input.required<string>();
  readonly description = input<string>('This feature will be implemented in a future milestone.');
}
