import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { AppHeaderComponent } from '../components/app-header/app-header.component';
import { SidebarComponent } from '../components/sidebar/sidebar.component';
import { NavigationItem } from '../models/navigation-item.model';

/**
 * Customer sidebar navigation (§ M4.3). Intentionally excludes UX Demo,
 * Customers (Admin-only), and any other Admin-facing feature.
 */
const CUSTOMER_NAVIGATION: readonly NavigationItem[] = [
  { label: 'Dashboard', route: '/customer/dashboard' },
  { label: 'My leases', route: '/customer/leases' },
  { label: 'Documents', route: '/customer/documents' },
  { label: 'My profile', route: '/customer/profile' },
];

/**
 * CustomerLayout — permanent application shell for authenticated CUSTOMER
 * users.
 *
 * Shares the same header/sidebar primitives and LeaseDemo design tokens as
 * AdminLayout; only the navigation items and routed content differ.
 */
@Component({
  selector: 'app-customer-layout',
  standalone: true,
  imports: [RouterOutlet, AppHeaderComponent, SidebarComponent],
  templateUrl: './customer-layout.component.html',
  styleUrl: './customer-layout.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CustomerLayoutComponent {
  protected readonly navigationItems = CUSTOMER_NAVIGATION;
}
