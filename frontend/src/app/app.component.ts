import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';

import { AuthService } from './core/services/auth.service';

/**
 * Root application component.
 *
 * Responsibilities:
 * - Provides the router outlet (application shell).
 * - Renders a minimal authentication status bar (Login/Logout) ONLY for
 *   routes outside the role-aware /admin and /customer application areas —
 *   those areas render their own shell header (AppHeaderComponent) via
 *   AdminLayout/CustomerLayout, so this top-level bar would otherwise stack
 *   a second, redundant header above them.
 *
 * Business logic lives in feature components and services, not here.
 */
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: `
    @if (!isInsideApplicationShell()) {
      <header class="app-header">
        <span class="app-header__brand">{{ title }}</span>

        <nav class="app-header__auth">
          @if (authService.isAuthenticated()) {
            <span class="app-header__username">{{ authService.username() }}</span>
            <span class="app-header__separator" aria-hidden="true">|</span>
            <button
              class="app-header__btn"
              type="button"
              (click)="authService.logout()"
            >
              Logout
            </button>
          } @else {
            <button
              class="app-header__btn"
              type="button"
              (click)="authService.login()"
            >
              Login
            </button>
          }
        </nav>
      </header>
    }

    <main class="app-shell">
      <router-outlet />
    </main>
  `,
  styles: [
    `
      .app-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 0.5rem 1.5rem;
        background: #1565c0;
        color: #fff;
      }

      .app-header__brand {
        font-weight: 600;
        font-size: 1rem;
        letter-spacing: 0.02em;
      }

      .app-header__auth {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        font-size: 0.9rem;
      }

      .app-header__username {
        font-weight: 500;
      }

      .app-header__separator {
        opacity: 0.5;
      }

      .app-header__btn {
        background: transparent;
        border: 1px solid rgba(255, 255, 255, 0.6);
        border-radius: 4px;
        color: #fff;
        cursor: pointer;
        font-size: 0.85rem;
        padding: 0.25rem 0.75rem;
      }

      .app-header__btn:hover {
        background: rgba(255, 255, 255, 0.15);
      }

      .app-shell {
        min-height: calc(100vh - 2.5rem);
        display: flex;
        flex-direction: column;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppComponent {
  protected readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  readonly title = 'Lease Demo';

  /** True when the current URL is inside /admin or /customer (own shell header). */
  protected readonly isInsideApplicationShell = signal(
    this.urlIsInsideApplicationShell(this.router.url)
  );

  constructor() {
    this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe((event) => {
        this.isInsideApplicationShell.set(this.urlIsInsideApplicationShell(event.urlAfterRedirects));
      });
  }

  private urlIsInsideApplicationShell(url: string): boolean {
    return url.startsWith('/admin') || url.startsWith('/customer');
  }
}
