import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { CustomerCreateRequest, CustomerProfile } from '../models/customer-profile.model';
import { CurrentCustomerService } from './current-customer.service';

describe('CurrentCustomerService', () => {
  let service: CurrentCustomerService;
  let httpMock: HttpTestingController;

  const sampleProfile: CustomerProfile = {
    id: '11111111-1111-1111-1111-111111111111',
    firstName: 'Jan',
    lastName: 'Kowalski',
    email: 'jan.kowalski@example.com',
    phoneNumber: '+48123456789',
    dateOfBirth: '1944-05-14',
    gender: 'MALE',
    createdAt: '2025-01-01T00:00:00Z',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CurrentCustomerService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('starts in the loading state', () => {
    expect(service.state()).toEqual({ status: 'loading' });
  });

  it('load(): 200 response transitions to found state with the profile', async () => {
    const promise = service.load();
    const req = httpMock.expectOne('/api/customers/me');
    expect(req.request.method).toBe('GET');
    req.flush(sampleProfile);
    await promise;

    expect(service.state()).toEqual({ status: 'found', profile: sampleProfile });
    expect(service.profile()).toEqual(sampleProfile);
  });

  it('load(): 404 response transitions to missing state (not an error)', async () => {
    const promise = service.load();
    const req = httpMock.expectOne('/api/customers/me');
    req.flush({ title: 'Customer Profile Not Found' }, { status: 404, statusText: 'Not Found' });
    await promise;

    expect(service.state()).toEqual({ status: 'missing' });
    expect(service.profile()).toBeNull();
  });

  it('load(): other HTTP errors transition to a safe error state', async () => {
    const promise = service.load();
    const req = httpMock.expectOne('/api/customers/me');
    req.flush({ title: 'Internal Server Error' }, { status: 500, statusText: 'Internal Server Error' });
    await promise;

    const state = service.state();
    expect(state.status).toBe('error');
  });

  it('createProfile(): posts to /customers without keycloakUserId and updates state to found', async () => {
    const request: CustomerCreateRequest = {
      firstName: 'Jan',
      lastName: 'Kowalski',
      email: 'jan.kowalski@example.com',
      phoneNumber: '+48123456789',
      dateOfBirth: '1944-05-14',
      gender: 'MALE',
      pesel: '44051401359',
    };

    const promise = service.createProfile(request);
    const req = httpMock.expectOne('/api/customers');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    expect((req.request.body as Record<string, unknown>)['keycloakUserId']).toBeUndefined();
    req.flush(sampleProfile);
    await promise;

    expect(service.state()).toEqual({ status: 'found', profile: sampleProfile });
  });
});
