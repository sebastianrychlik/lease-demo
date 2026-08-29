import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * LeaseDemo application-level card surface.
 *
 * A simple, restrained corporate container: white surface, subtle border,
 * small border radius, subtle shadow, sensible padding. Intentionally has
 * no configuration inputs — compose content via projection. Use Tailwind
 * utility classes on the host (e.g. `class="flex flex-col gap-2"`) for
 * layout of the projected content when needed.
 *
 * Usage:
 * ```html
 * <app-card>
 *   <h3>Lease #LD-10234</h3>
 *   <p>2023 Volvo XC60 — Active</p>
 * </app-card>
 * ```
 */
@Component({
  selector: 'app-card',
  standalone: true,
  template: `
    <div class="app-card">
      <ng-content></ng-content>
    </div>
  `,
  styleUrl: './card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CardComponent {}
