import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { CustomerListItem } from '../../models/customer-list-item.model';
import { PageResponse } from '../../models/page-response.model';
import { CustomerService } from '../../services/customer.service';
import { CustomerListPageComponent } from './customer-list-page.component';

function item(overrides: Partial<CustomerListItem> = {}): CustomerListItem {
  return {
    id: '11111111-1111-1111-1111-111111111111',
    firstName: 'Jan',
    lastName: 'Kowalski',
    email: 'jan.kowalski@example.com',
    phoneNumber: '+48123456789',
    dateOfBirth: '1944-05-14',
    gender: 'MALE',
    createdAt: '2025-01-01T00:00:00Z',
    ...overrides,
  };
}

function page(content: CustomerListItem[], totalElements = content.length): PageResponse<CustomerListItem> {
  return { content, page: 0, size: 20, totalElements, totalPages: 1, first: true, last: true };
}

describe('CustomerListPageComponent', () => {
  let fixture: ComponentFixture<CustomerListPageComponent>;
  let customerServiceSpy: jasmine.SpyObj<CustomerService>;

  beforeEach(async () => {
    customerServiceSpy = jasmine.createSpyObj<CustomerService>('CustomerService', ['getCustomers']);
    customerServiceSpy.getCustomers.and.returnValue(of(page([item()])));

    await TestBed.configureTestingModule({
      imports: [CustomerListPageComponent, NoopAnimationsModule],
      providers: [{ provide: CustomerService, useValue: customerServiceSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(CustomerListPageComponent);
  });

  it('loads and renders customers on init', fakeAsync(() => {
    fixture.detectChanges();
    tick(400);
    fixture.detectChanges();

    expect(customerServiceSpy.getCustomers).toHaveBeenCalled();
    const rows = fixture.nativeElement.querySelectorAll('app-data-table tbody tr');
    expect(rows.length).toBe(1);
  }));

  it('shows a loading state before the first response resolves', () => {
    fixture.detectChanges();
    expect(fixture.componentInstance.isLoading()).toBeTrue();
  });

  it('debounces and resets to page 0 on search input', fakeAsync(() => {
    fixture.detectChanges();
    tick(400);
    customerServiceSpy.getCustomers.calls.reset();

    fixture.componentInstance.page.set(3);
    fixture.componentInstance.searchControl.setValue('kowalski');
    tick(400);
    fixture.detectChanges();

    expect(fixture.componentInstance.page()).toBe(0);
    const lastQuery = customerServiceSpy.getCustomers.calls.mostRecent().args[0];
    expect(lastQuery.search).toBe('kowalski');
  }));

  it('issues a new server request when the page changes', fakeAsync(() => {
    fixture.detectChanges();
    tick(400);
    customerServiceSpy.getCustomers.calls.reset();

    fixture.componentInstance.onPageChange({ pageIndex: 2, pageSize: 20, length: 100 });
    tick(400);

    const lastQuery = customerServiceSpy.getCustomers.calls.mostRecent().args[0];
    expect(lastQuery.page).toBe(2);
  }));

  it('issues a new server request when sort changes and resets the page', fakeAsync(() => {
    fixture.detectChanges();
    tick(400);
    fixture.componentInstance.page.set(3);
    customerServiceSpy.getCustomers.calls.reset();

    fixture.componentInstance.onSortChange({ field: 'email', direction: 'desc' });
    tick(400);

    expect(fixture.componentInstance.page()).toBe(0);
    const lastQuery = customerServiceSpy.getCustomers.calls.mostRecent().args[0];
    expect(lastQuery.sortField).toBe('email');
    expect(lastQuery.sortDirection).toBe('desc');
  }));

  it('shows an error state when the API call fails', fakeAsync(() => {
    customerServiceSpy.getCustomers.and.returnValue(throwError(() => new Error('boom')));
    fixture.detectChanges();
    tick(400);
    fixture.detectChanges();

    expect(fixture.componentInstance.viewState().status).toBe('error');
  }));

  it('shows an empty state when the response has no customers', fakeAsync(() => {
    customerServiceSpy.getCustomers.and.returnValue(of(page([], 0)));
    fixture.detectChanges();
    tick(400);
    fixture.detectChanges();

    expect(fixture.componentInstance.viewState().status).toBe('empty-no-customers');
  }));
});
