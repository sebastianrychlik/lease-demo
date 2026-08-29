import { Routes } from '@angular/router';

/**
 * UX Demo routes.
 *
 * FUTURE: once role-aware Admin/Customer application shells exist, /ux-demo
 * must be reachable only from Admin/developer-facing navigation, and NEVER
 * exposed in Customer-facing navigation. It is intentionally left directly
 * routable in this milestone because AdminLayout/CustomerLayout do not exist
 * yet — see docs/milestones/M4_Persistence_and_Lease_Domain.md (§ M4.2).
 */
export const UX_DEMO_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/ux-demo-page/ux-demo-page.component').then(
        (m) => m.UxDemoPageComponent
      ),
  },
];
