import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { AuthService } from '../../../core/services/auth.service';

/**
 * Shared LeaseDemo application-shell header.
 *
 * Used by both AdminLayout and CustomerLayout so the two application areas
 * clearly belong to the same product. Shows the LeaseDemo brand, the
 * authenticated user's display name (Keycloak `preferred_username` — no
 * backend call, no raw token data rendered), and a Logout action that
 * delegates to the existing `AuthService.logout()` (full Keycloak session
 * termination), never reimplemented locally.
 */
@Component({
  selector: 'app-header',
  standalone: true,
  templateUrl: './app-header.component.html',
  styleUrl: './app-header.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppHeaderComponent {
  protected readonly authService = inject(AuthService);
}
