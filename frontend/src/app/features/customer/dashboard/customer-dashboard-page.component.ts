import { ChangeDetectionStrategy, Component } from '@angular/core';

import { CardComponent, PageHeaderComponent } from '../../../shared/ui';

/**
 * Customer Dashboard — shell-validation landing page for authenticated
 * CUSTOMER users. Intentionally static: no backend calls, no real lease
 * data. Demonstrates layout composition using shared/ui only.
 */
@Component({
  selector: 'app-customer-dashboard-page',
  standalone: true,
  imports: [PageHeaderComponent, CardComponent],
  templateUrl: './customer-dashboard-page.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CustomerDashboardPageComponent {}
