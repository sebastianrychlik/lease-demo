import { Routes } from '@angular/router';

import { CustomerLayoutComponent } from '../../layout/customer-layout/customer-layout.component';

/**
 * Customer feature routes — all wrapped in {@link CustomerLayoutComponent}.
 *
 * Role protection (CUSTOMER-required) is applied once, on the parent
 * `/customer` route in app.routes.ts (`customerAreaGuard`).
 */
export const CUSTOMER_ROUTES: Routes = [
  {
    path: '',
    component: CustomerLayoutComponent,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./dashboard/customer-dashboard-page.component').then(
            (m) => m.CustomerDashboardPageComponent
          ),
      },
      {
        path: 'leases',
        loadComponent: () =>
          import('../../shared/components/feature-placeholder/feature-placeholder.component').then(
            (m) => m.FeaturePlaceholderComponent
          ),
        data: { title: 'My leases' },
      },
      {
        path: 'documents',
        loadComponent: () =>
          import('../../shared/components/feature-placeholder/feature-placeholder.component').then(
            (m) => m.FeaturePlaceholderComponent
          ),
        data: { title: 'Documents' },
      },
      {
        path: 'profile',
        loadComponent: () =>
          import('../../shared/components/feature-placeholder/feature-placeholder.component').then(
            (m) => m.FeaturePlaceholderComponent
          ),
        data: { title: 'My profile' },
      },
    ],
  },
];
