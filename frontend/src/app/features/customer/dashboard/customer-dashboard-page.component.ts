import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';

import { CardComponent, PageHeaderComponent } from '../../../shared/ui';
import { CurrentCustomerService } from '../profile/services/current-customer.service';

/**
 * Customer Dashboard — landing page for authenticated CUSTOMER users with a
 * persisted domain profile (M4.5; `requireCustomerProfileGuard` guarantees
 * this before the route is reachable).
 *
 * Reads the already-loaded profile from {@link CurrentCustomerService}
 * (no duplicate `GET /api/customers/me` call). Deliberately shows no lease
 * counts or financial metrics — those are not implemented yet.
 */
@Component({
  selector: 'app-customer-dashboard-page',
  standalone: true,
  imports: [PageHeaderComponent, CardComponent, TranslocoModule],
  templateUrl: './customer-dashboard-page.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CustomerDashboardPageComponent {
  private readonly currentCustomerService = inject(CurrentCustomerService);
  private readonly translocoService = inject(TranslocoService);

  private readonly activeLang = toSignal(this.translocoService.langChanges$, {
    initialValue: this.translocoService.getActiveLang(),
  });

  readonly welcomeSubtitle = computed(() => {
    this.activeLang();
    const profile = this.currentCustomerService.profile();
    return profile
      ? this.translocoService.translate('customer.dashboard.welcomeWithName', { name: profile.firstName })
      : this.translocoService.translate('customer.dashboard.welcomeGeneric');
  });
}
