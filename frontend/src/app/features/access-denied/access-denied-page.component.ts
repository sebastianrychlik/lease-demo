import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { AuthService } from '../../core/services/auth.service';
import { ButtonComponent, CardComponent, PageHeaderComponent } from '../../shared/ui';

/**
 * Access Denied page.
 *
 * Shown for:
 * - an authenticated user whose Keycloak roles map to no recognized
 *   LeaseDemo application role (fail-closed — see RoleService);
 * - role-inappropriate route access (e.g. CUSTOMER navigating to /admin/**).
 *
 * Deliberately shows no internal authorization detail or JWT claims — only
 * a plain-language explanation and a safe logout action.
 */
@Component({
  selector: 'app-access-denied-page',
  standalone: true,
  imports: [PageHeaderComponent, CardComponent, ButtonComponent],
  templateUrl: './access-denied-page.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AccessDeniedPageComponent {
  protected readonly authService = inject(AuthService);
}
