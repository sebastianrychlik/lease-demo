/**
 * A single sidebar navigation entry.
 *
 * Both AdminLayout and CustomerLayout provide their own typed array of
 * `NavigationItem`s to the shared `SidebarComponent` rather than the
 * sidebar hardcoding per-area navigation.
 */
export interface NavigationItem {
  /** Transloco translation key for the visible link label (static UI text — M5.1.3). */
  readonly label: string;
  /** Router route (absolute, e.g. `/admin/dashboard`). */
  readonly route: string;
  /** Material icon ligature name (from `MatIconModule`), if available. */
  readonly icon?: string;
}
