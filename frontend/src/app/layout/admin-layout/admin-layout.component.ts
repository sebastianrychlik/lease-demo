import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { AppHeaderComponent } from '../components/app-header/app-header.component';
import { SidebarComponent } from '../components/sidebar/sidebar.component';
import { NavigationItem } from '../models/navigation-item.model';

/** Admin sidebar navigation (§ M4.3). */
const ADMIN_NAVIGATION: readonly NavigationItem[] = [
  { label: 'admin.nav.dashboard', route: '/admin/dashboard' },
  { label: 'admin.nav.customers', route: '/admin/customers' },
  { label: 'admin.nav.leases', route: '/admin/leases' },
];

/**
 * Admin/developer-only navigation, visually separated below the primary
 * group. `/ux-demo` stays reachable only from here — never from
 * CustomerLayout.
 */
const ADMIN_SECONDARY_NAVIGATION: readonly NavigationItem[] = [
  { label: 'admin.nav.uxDemo', route: '/ux-demo' },
];

/**
 * AdminLayout — permanent application shell for authenticated ADMIN users.
 *
 * Provides the header + sidebar + `router-outlet` composition for every
 * `/admin/**` route. Uses the same LeaseDemo design system as
 * CustomerLayout (shared header/sidebar components, shared design tokens) —
 * differences between the two shells are navigation/content only.
 */
@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [RouterOutlet, AppHeaderComponent, SidebarComponent],
  templateUrl: './admin-layout.component.html',
  styleUrl: './admin-layout.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminLayoutComponent {
  protected readonly navigationItems = ADMIN_NAVIGATION;
  protected readonly secondaryNavigationItems = ADMIN_SECONDARY_NAVIGATION;
}
