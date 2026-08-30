import { Routes } from '@angular/router';

import { AdminLayoutComponent } from '../../layout/admin-layout/admin-layout.component';

/**
 * Admin feature routes — all wrapped in {@link AdminLayoutComponent}.
 *
 * Role protection (ADMIN-only) is applied once, on the parent `/admin`
 * route in app.routes.ts (`adminAreaGuard`), rather than duplicated on
 * every child route.
 */
export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    component: AdminLayoutComponent,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./dashboard/admin-dashboard-page.component').then(
            (m) => m.AdminDashboardPageComponent
          ),
      },
      {
        path: 'customers',
        loadComponent: () =>
          import('./customers/pages/customer-list/customer-list-page.component').then(
            (m) => m.CustomerListPageComponent
          ),
        data: { title: 'Customers' },
      },
      {
        path: 'lease-products',
        loadComponent: () =>
          import('./lease-products/pages/lease-product-list/lease-product-list-page.component').then(
            (m) => m.LeaseProductListPageComponent
          ),
        data: { title: 'Lease Products' },
      },
      {
        path: 'lease-products/new',
        loadComponent: () =>
          import('./lease-products/pages/lease-product-editor/lease-product-editor-page.component').then(
            (m) => m.LeaseProductEditorPageComponent
          ),
        data: { title: 'Add Lease Product' },
      },
      {
        path: 'lease-products/:code/edit',
        loadComponent: () =>
          import('./lease-products/pages/lease-product-editor/lease-product-editor-page.component').then(
            (m) => m.LeaseProductEditorPageComponent
          ),
        data: { title: 'Edit Lease Product' },
      },
      {
        path: 'leases',
        loadComponent: () =>
          import('../../shared/components/feature-placeholder/feature-placeholder.component').then(
            (m) => m.FeaturePlaceholderComponent
          ),
        data: { title: 'Leases' },
      },
    ],
  },
];
