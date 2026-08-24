import { AppRole, mapKeycloakRolesToAppRoles } from './app-role.model';

describe('mapKeycloakRolesToAppRoles', () => {
  it('maps the ADMIN realm role to AppRole.Admin', () => {
    expect(mapKeycloakRolesToAppRoles(['ADMIN'])).toEqual([AppRole.Admin]);
  });

  it('maps the CUSTOMER realm role to AppRole.Customer', () => {
    expect(mapKeycloakRolesToAppRoles(['CUSTOMER'])).toEqual([AppRole.Customer]);
  });

  it('maps both roles when both are present', () => {
    expect(mapKeycloakRolesToAppRoles(['ADMIN', 'CUSTOMER'])).toEqual([
      AppRole.Admin,
      AppRole.Customer,
    ]);
  });

  it('ignores unrecognized realm roles (e.g. ADVISOR, offline_access)', () => {
    expect(mapKeycloakRolesToAppRoles(['ADVISOR', 'offline_access', 'uma_authorization'])).toEqual(
      []
    );
  });

  it('returns an empty array for no roles', () => {
    expect(mapKeycloakRolesToAppRoles([])).toEqual([]);
  });
});
