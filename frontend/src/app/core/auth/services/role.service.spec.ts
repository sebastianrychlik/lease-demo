import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';

import { RoleService } from './role.service';
import { AuthService } from '../../services/auth.service';

describe('RoleService', () => {
  let rolesSignal: ReturnType<typeof signal<string[]>>;

  function configure(): RoleService {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: AuthService,
          useValue: { roles: rolesSignal },
        },
      ],
    });
    return TestBed.inject(RoleService);
  }

  beforeEach(() => {
    rolesSignal = signal<string[]>([]);
  });

  it('resolves ADMIN to the Admin area', () => {
    rolesSignal.set(['ADMIN']);
    const service = configure();

    expect(service.isAdmin()).toBeTrue();
    expect(service.isCustomer()).toBeFalse();
    expect(service.resolveLandingRoute()).toBe('/admin/dashboard');
  });

  it('resolves CUSTOMER to the Customer area', () => {
    rolesSignal.set(['CUSTOMER']);
    const service = configure();

    expect(service.isCustomer()).toBeTrue();
    expect(service.isAdmin()).toBeFalse();
    expect(service.resolveLandingRoute()).toBe('/customer/dashboard');
  });

  it('ADMIN wins the default landing area when both roles are present', () => {
    rolesSignal.set(['ADMIN', 'CUSTOMER']);
    const service = configure();

    expect(service.isAdmin()).toBeTrue();
    expect(service.isCustomer()).toBeTrue();
    expect(service.resolveLandingRoute()).toBe('/admin/dashboard');
  });

  it('fails closed for an unrecognized role', () => {
    rolesSignal.set(['ADVISOR']);
    const service = configure();

    expect(service.isAdmin()).toBeFalse();
    expect(service.isCustomer()).toBeFalse();
    expect(service.hasNoRecognizedRole()).toBeTrue();
    expect(service.resolveLandingRoute()).toBeNull();
  });

  it('fails closed when the user has no roles at all', () => {
    rolesSignal.set([]);
    const service = configure();

    expect(service.hasNoRecognizedRole()).toBeTrue();
    expect(service.resolveLandingRoute()).toBeNull();
  });
});
