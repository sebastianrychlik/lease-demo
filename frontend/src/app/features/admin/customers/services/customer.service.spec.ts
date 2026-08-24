import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';

import { CustomerListItem } from '../models/customer-list-item.model';
import { CustomerQuery } from '../models/customer-query.model';
import { PageResponse } from '../models/page-response.model';
import { CustomerService } from './customer.service';

describe('CustomerService', () => {
  let service: CustomerService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CustomerService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('builds query parameters for a minimal query (no search/sort)', () => {
    const query: CustomerQuery = { page: 0, size: 20, search: null, sortField: null, sortDirection: null };

    service.getCustomers(query).subscribe();

    const req = httpMock.expectOne(
      (r) => r.url === '/api/customers' && r.params.get('page') === '0' && r.params.get('size') === '20',
    );
    expect(req.request.params.has('search')).toBeFalse();
    expect(req.request.params.has('sortField')).toBeFalse();
    req.flush({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, first: true, last: true });
  });

  it('includes search and sort parameters when provided', () => {
    const query: CustomerQuery = {
      page: 1,
      size: 50,
      search: 'kowalski',
      sortField: 'lastName',
      sortDirection: 'desc',
    };

    service.getCustomers(query).subscribe();

    const req = httpMock.expectOne((r) => r.url === '/api/customers');
    expect(req.request.params.get('page')).toBe('1');
    expect(req.request.params.get('size')).toBe('50');
    expect(req.request.params.get('search')).toBe('kowalski');
    expect(req.request.params.get('sortField')).toBe('lastName');
    expect(req.request.params.get('sortDirection')).toBe('desc');
    req.flush({ content: [], page: 1, size: 50, totalElements: 0, totalPages: 0, first: true, last: true });
  });

  it('returns a typed PageResponse<CustomerListItem>', (done) => {
    const query: CustomerQuery = { page: 0, size: 20, search: null, sortField: null, sortDirection: null };
    const item: CustomerListItem = {
      id: '11111111-1111-1111-1111-111111111111',
      firstName: 'Jan',
      lastName: 'Kowalski',
      email: 'jan.kowalski@example.com',
      phoneNumber: '+48123456789',
      dateOfBirth: '1944-05-14',
      gender: 'MALE',
      createdAt: '2025-01-01T00:00:00Z',
    };
    const response: PageResponse<CustomerListItem> = {
      content: [item],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
    };

    service.getCustomers(query).subscribe((result) => {
      expect(result).toEqual(response);
      done();
    });

    const req = httpMock.expectOne((r) => r.url === '/api/customers');
    req.flush(response);
  });
});
