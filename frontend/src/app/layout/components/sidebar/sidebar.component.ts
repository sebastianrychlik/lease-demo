import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { NavigationItem } from '../../models/navigation-item.model';

/**
 * Shared corporate sidebar navigation.
 *
 * Renders a typed list of {@link NavigationItem}s and, optionally, a second
 * visually-separated group (used by AdminLayout for the "UX Demo"
 * developer-facing link). Active route is indicated via `routerLinkActive`.
 *
 * Deliberately NOT a generic menu framework: no nesting, no dynamic icon
 * font dependency (LeaseDemo does not currently ship a Material icon font —
 * see the UX Demo note on `mat-icon`), no per-item role logic. AdminLayout
 * and CustomerLayout each own their own navigation item arrays and simply
 * pass different data into the same component.
 */
@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SidebarComponent {
  /** Primary navigation items (e.g. Dashboard, Customers, Leases). */
  readonly items = input.required<readonly NavigationItem[]>();

  /**
   * Optional secondary, visually-separated navigation group (e.g. Admin's
   * "UX Demo" developer link). Omitted entirely by CustomerLayout.
   */
  readonly secondaryItems = input<readonly NavigationItem[]>([]);
}
