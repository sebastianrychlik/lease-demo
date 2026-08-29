import { ChangeDetectionStrategy, Component } from '@angular/core';

import { CardComponent, PageHeaderComponent } from '../../../shared/ui';

/**
 * Admin Dashboard — shell-validation landing page for authenticated ADMIN
 * users. Intentionally static: no charts, no backend calls, no business
 * metrics. Demonstrates layout composition using shared/ui only.
 */
@Component({
  selector: 'app-admin-dashboard-page',
  standalone: true,
  imports: [PageHeaderComponent, CardComponent],
  templateUrl: './admin-dashboard-page.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminDashboardPageComponent {}
