import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSliderModule } from '@angular/material/slider';

import { LeaseProductConfiguration } from '../../models/lease-quote.model';
import { LeaseQuoteFormControls } from '../../pages/lease-quote-page.component';

/**
 * Presentational lease parameters form.
 *
 * Renders the vehicle price / currency / term / initial payment / buyout /
 * lease type controls, all bound to the `FormGroup` owned by the parent
 * `lease-quote-page`. Every option/range (currencies, terms, initial
 * payment / buyout ranges, lease types) is rendered from the backend-driven
 * `product` input (M5.1.2) — nothing is hard-coded here. Contains no
 * calculation logic, no HTTP calls, and no knowledge of the resulting
 * quote — purely a reactive-forms view.
 */
@Component({
  selector: 'app-lease-parameters',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonToggleModule,
    MatSliderModule,
  ],
  templateUrl: './lease-parameters.component.html',
  styleUrl: './lease-parameters.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LeaseParametersComponent {
  /** The lease quote form, owned and constructed by the page component. */
  readonly formGroup = input.required<FormGroup<LeaseQuoteFormControls>>();

  /** The selected Lease Product configuration — the sole source of rendered options/ranges. */
  readonly product = input.required<LeaseProductConfiguration>();
}
