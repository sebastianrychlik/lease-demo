import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { adminAreaGuard, customerAreaGuard } from './role.guard';
import { RoleService } from '../services/role.service';

describe('role guards', () => {
  const mockRoute = {} as ActivatedRouteSnapshot;
  const mockState = {} as RouterStateSnapshot;

  let isAdminValue: boolean;
  let isCustomerValue: boolean;

  function configure(): void {
    TestBed.configureTestingModule({
      imports: [RouterTestingModule],
      providers: [
        {
          provide: RoleService,
          useValue: {
            isAdmin: () => isAdminValue,
            isCustomer: () => isCustomerValue,
          },
        },
      ],
    });
  }

  beforeEach(() => {
    isAdminValue = false;
    isCustomerValue = false;
  });

  describe('adminAreaGuard', () => {
    it('allows ADMIN users to access /admin/**', () => {
      isAdminValue = true;
      configure();
      const result = TestBed.runInInjectionContext(() => adminAreaGuard(mockRoute, mockState));
      expect(result).toBeTrue();
    });

    it('denies CUSTOMER-only users access to /admin/** (redirects to /access-denied)', () => {
      isCustomerValue = true;
      configure();
      const result = TestBed.runInInjectionContext(() =>
        adminAreaGuard(mockRoute, mockState)
      ) as UrlTree;
      expect(result.toString()).toBe('/access-denied');
    });
  });

  describe('customerAreaGuard', () => {
    it('allows CUSTOMER users to access /customer/**', () => {
      isCustomerValue = true;
      configure();
      const result = TestBed.runInInjectionContext(() => customerAreaGuard(mockRoute, mockState));
      expect(result).toBeTrue();
    });

    it('denies ADMIN-only users access to /customer/** unless they also hold CUSTOMER', () => {
      isAdminValue = true;
      configure();
      const result = TestBed.runInInjectionContext(() =>
        customerAreaGuard(mockRoute, mockState)
      ) as UrlTree;
      expect(result.toString()).toBe('/access-denied');
    });

    it('allows an ADMIN who also holds CUSTOMER to access /customer/**', () => {
      isAdminValue = true;
      isCustomerValue = true;
      configure();
      const result = TestBed.runInInjectionContext(() => customerAreaGuard(mockRoute, mockState));
      expect(result).toBeTrue();
    });
  });
});
