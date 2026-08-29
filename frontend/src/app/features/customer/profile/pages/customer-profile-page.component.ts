import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { CardComponent, PageHeaderComponent } from '../../../../shared/ui';
import { CurrentCustomerService } from '../services/current-customer.service';

/**
 * My Profile — /customer/profile (M4.5).
 *
 * Read-only display of the authenticated CUSTOMER's persisted profile,
 * sourced from the shared {@link CurrentCustomerService} cache (no
 * duplicate `GET /api/customers/me` call here — `requireCustomerProfileGuard`
 * already guarantees a `found` state before this route is reachable).
 *
 * PESEL is never displayed in plaintext — only a restrained "Registered"
 * status, matching the backend's deliberate exclusion of PESEL (raw,
 * encrypted, or lookup hash) from the API response.
 */
@Component({
  selector: 'app-customer-profile-page',
  standalone: true,
  imports: [PageHeaderComponent, CardComponent],
  templateUrl: './customer-profile-page.component.html',
  styleUrl: './customer-profile-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CustomerProfilePageComponent {
  private readonly currentCustomerService = inject(CurrentCustomerService);

  readonly profile = this.currentCustomerService.profile;
}
