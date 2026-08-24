import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { ButtonComponent, CardComponent, InputComponent, PageHeaderComponent } from '@shared/ui';

import { DEMO_LEASE_CUSTOMERS } from '../../data/ux-demo.data';

/**
 * UX Demo — LeaseDemo's permanent living design-system catalog.
 *
 * Consumes the REAL components from `src/app/shared/ui` (never duplicates
 * them). Exists to visually verify LeaseDemo's Material + Tailwind + shared
 * UI foundation and to document supported variants/states through working
 * examples. See docs/milestones/M4_Persistence_and_Lease_Domain.md (§ M4.2).
 *
 * Note: `mat-progress-spinner` (SVG-based) is used for the Material
 * integration example rather than `mat-icon`, which normally depends on an
 * external icon font/network dependency LeaseDemo does not currently ship.
 */
@Component({
  selector: 'app-ux-demo-page',
  standalone: true,
  imports: [ButtonComponent, CardComponent, PageHeaderComponent, InputComponent, MatProgressSpinnerModule],
  templateUrl: './ux-demo-page.component.html',
  styleUrl: './ux-demo-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UxDemoPageComponent {
  readonly demoCustomers = DEMO_LEASE_CUSTOMERS;
}
