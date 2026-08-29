import { APP_ROUTES } from './app.routes';
import { adminAreaGuard, customerAreaGuard } from './core/auth/guards/role.guard';

/**
 * Verifies the route-level wiring of role guards without exercising the
 * Angular Router itself (per instruction: don't test Router internals).
 */
describe('APP_ROUTES role-guard wiring', () => {
  function findRoute(path: string) {
    const route = APP_ROUTES.find((r) => r.path === path);
    if (!route) {
      throw new Error(`Route '${path}' not found`);
    }
    return route;
  }

  it('protects /admin with adminAreaGuard', () => {
    expect(findRoute('admin').canActivate).toContain(adminAreaGuard);
  });

  it('protects /customer with customerAreaGuard', () => {
    expect(findRoute('customer').canActivate).toContain(customerAreaGuard);
  });

  it('protects /ux-demo with adminAreaGuard so ADMIN can access it and CUSTOMER cannot', () => {
    expect(findRoute('ux-demo').canActivate).toContain(adminAreaGuard);
  });
});
