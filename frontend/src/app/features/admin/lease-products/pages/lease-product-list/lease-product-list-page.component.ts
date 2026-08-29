import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { TranslocoModule } from '@jsverse/transloco';
import { catchError, of } from 'rxjs';

import { ButtonComponent, CardComponent, PageHeaderComponent } from '../../../../../shared/ui';
import { AdminLeaseProduct } from '../../models/admin-lease-product.model';
import { AdminLeaseProductService } from '../../services/admin-lease-product.service';

/** Discriminated union representing all possible UI states for the list. */
type LeaseProductListViewState =
  | { status: 'loading' }
  | { status: 'success'; products: AdminLeaseProduct[] }
  | { status: 'empty' }
  | { status: 'error' };

/**
 * ADMIN Lease Product list page — /admin/lease-products (M5.1.4).
 *
 * Shows ALL products (enabled/disabled/valid/expired) via the ADMIN list
 * endpoint — never the CUSTOMER "available" filtering.
 */
@Component({
  selector: 'app-lease-product-list-page',
  standalone: true,
  imports: [CommonModule, RouterLink, PageHeaderComponent, CardComponent, ButtonComponent, TranslocoModule],
  templateUrl: './lease-product-list-page.component.html',
  styleUrl: './lease-product-list-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LeaseProductListPageComponent implements OnInit {
  private readonly adminLeaseProductService = inject(AdminLeaseProductService);
  private readonly router = inject(Router);

  readonly viewState = signal<LeaseProductListViewState>({ status: 'loading' });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.viewState.set({ status: 'loading' });
    this.adminLeaseProductService
      .getAllProducts()
      .pipe(
        catchError(() => {
          this.viewState.set({ status: 'error' });
          return of(null);
        }),
      )
      .subscribe((products) => {
        if (!products) {
          return;
        }
        this.viewState.set(products.length === 0 ? { status: 'empty' } : { status: 'success', products });
      });
  }

  addProduct(): void {
    this.router.navigate(['/admin/lease-products/new']);
  }

  editProduct(code: string): void {
    this.router.navigate(['/admin/lease-products', code, 'edit']);
  }
}
