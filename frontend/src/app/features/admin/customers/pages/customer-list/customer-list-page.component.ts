import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import {
  catchError,
  combineLatest,
  debounceTime,
  distinctUntilChanged,
  of,
  startWith,
  switchMap,
} from 'rxjs';

import {
  CardComponent,
  DataTableColumn,
  DataTableComponent,
  DataTableSortEvent,
  InputComponent,
  PageHeaderComponent,
} from '../../../../../shared/ui';
import { CustomerListItem } from '../../models/customer-list-item.model';
import {
  CustomerQuery,
  CustomerSortField,
  DEFAULT_CUSTOMER_QUERY,
  SortDirection,
} from '../../models/customer-query.model';
import { PageResponse } from '../../models/page-response.model';
import { CustomerService } from '../../services/customer.service';

/** Discriminated union representing all possible UI states for the results area. */
type CustomerListViewState =
  | { status: 'loading' }
  | { status: 'success'; data: PageResponse<CustomerListItem> }
  | { status: 'empty-no-customers' }
  | { status: 'empty-no-matches' }
  | { status: 'error'; message: string };

const SEARCH_DEBOUNCE_MS = 300;

/**
 * Admin → Customers page (M4.4).
 *
 * Server-side paged/sorted/searched Customer list. Reacts to search, page,
 * and sort changes via a single composed RxJS pipeline
 * (`combineLatest` + `switchMap`), so an obsolete in-flight request can
 * never overwrite a newer one. PostgreSQL performs pagination, sorting,
 * and filtering — this component never loads more than one page of rows.
 */
@Component({
  selector: 'app-customer-list-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    PageHeaderComponent,
    CardComponent,
    InputComponent,
    DataTableComponent,
    MatPaginatorModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './customer-list-page.component.html',
  styleUrl: './customer-list-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CustomerListPageComponent implements OnInit {
  private readonly customerService = inject(CustomerService);

  readonly searchControl = new FormControl<string>('', { nonNullable: true });

  readonly page = signal(DEFAULT_CUSTOMER_QUERY.page);
  readonly pageSize = signal(DEFAULT_CUSTOMER_QUERY.size);
  readonly sortField = signal<CustomerSortField | null>(DEFAULT_CUSTOMER_QUERY.sortField);
  readonly sortDirection = signal<SortDirection | null>(DEFAULT_CUSTOMER_QUERY.sortDirection);

  readonly viewState = signal<CustomerListViewState>({ status: 'loading' });

  readonly columns: DataTableColumn<CustomerListItem>[] = [
    {
      key: 'lastName',
      label: 'Customer',
      sortable: true,
      cell: (row) => `${row.firstName} ${row.lastName}`,
    },
    { key: 'email', label: 'Email', sortable: true },
    { key: 'phoneNumber', label: 'Phone', cell: (row) => row.phoneNumber ?? '—' },
    { key: 'dateOfBirth', label: 'Date of birth', sortable: true },
    { key: 'gender', label: 'Gender' },
    {
      key: 'createdAt',
      label: 'Created',
      cell: (row) => new Date(row.createdAt).toLocaleDateString(),
    },
  ];

  readonly rows = computed<CustomerListItem[]>(() => {
    const state = this.viewState();
    return state.status === 'success' ? state.data.content : [];
  });

  readonly totalElements = computed<number>(() => {
    const state = this.viewState();
    return state.status === 'success' ? state.data.totalElements : 0;
  });

  readonly isLoading = computed(() => this.viewState().status === 'loading');

  // toObservable() must run within an injection context — as field
  // initializers (constructor-time), not inside ngOnInit.
  private readonly page$ = toObservable(this.page);
  private readonly pageSize$ = toObservable(this.pageSize);
  private readonly sortField$ = toObservable(this.sortField);
  private readonly sortDirection$ = toObservable(this.sortDirection);

  ngOnInit(): void {
    const search$ = this.searchControl.valueChanges.pipe(
      startWith(this.searchControl.value),
      debounceTime(SEARCH_DEBOUNCE_MS),
      distinctUntilChanged(),
    );

    combineLatest([
      search$,
      this.page$,
      this.pageSize$,
      this.sortField$,
      this.sortDirection$,
    ])
      .pipe(
        switchMap(([search, page, pageSize, sortField, sortDirection]) => {
          this.viewState.set({ status: 'loading' });
          const query: CustomerQuery = {
            page,
            size: pageSize,
            search: search.trim() ? search.trim() : null,
            sortField,
            sortDirection,
          };
          return this.customerService.getCustomers(query).pipe(
            catchError((err: unknown) => {
              const message =
                err instanceof Error ? err.message : 'Unable to reach the backend.';
              this.viewState.set({ status: 'error', message });
              return of(null);
            }),
          );
        }),
      )
      .subscribe((data) => {
        if (!data) {
          return;
        }
        if (data.totalElements === 0) {
          this.viewState.set(
            this.searchControl.value.trim()
              ? { status: 'empty-no-matches' }
              : { status: 'empty-no-customers' },
          );
          return;
        }
        this.viewState.set({ status: 'success', data });
      });

    // Search changes reset to page 0 — a new search criteria makes the
    // previous page position meaningless.
    this.searchControl.valueChanges.subscribe(() => this.page.set(0));
  }

  onSortChange(event: DataTableSortEvent): void {
    this.page.set(0);
    this.sortField.set(event.field as CustomerSortField);
    this.sortDirection.set(event.direction);
  }

  onPageChange(event: PageEvent): void {
    this.page.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
  }

  retry(): void {
    // Nudges the search FormControl to re-emit its current value, which is
    // part of the combined pipeline above and therefore re-issues the request.
    this.searchControl.setValue(this.searchControl.value);
  }
}


