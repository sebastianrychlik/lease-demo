import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatSort, MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';

import { DataTableColumn, DataTableSortDirection, DataTableSortEvent } from './data-table.model';

/**
 * LeaseDemo generic reusable data table.
 *
 * Wraps Angular Material's table + sort-header primitives (semantic table
 * markup, accessible sort-state announcement) with a small, LeaseDemo-level
 * API driven entirely by column definitions/rows supplied by the caller.
 *
 * `app-data-table` intentionally has NO knowledge of any business domain
 * (e.g. Customer) — business-specific column definitions/rendering belong
 * to the consuming feature.
 *
 * Sorting is always reported upward via `(sortChange)`; this component does
 * NOT sort the supplied rows itself — callers own server-side sorting (see
 * Admin Customer list page) or, for purely local/demo use, may re-sort the
 * rows themselves before passing them in.
 *
 * Usage:
 * ```html
 * <app-data-table
 *   [columns]="columns"
 *   [rows]="rows()"
 *   [loading]="loading()"
 *   [activeSortField]="sortField()"
 *   [activeSortDirection]="sortDirection()"
 *   (sortChange)="onSort($event)"
 * ></app-data-table>
 * ```
 */
@Component({
  selector: 'app-data-table',
  standalone: true,
  imports: [MatTableModule, MatSortModule],
  templateUrl: './data-table.component.html',
  styleUrl: './data-table.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DataTableComponent<T> {
  /** Column definitions — order determines visual column order. */
  readonly columns = input.required<DataTableColumn<T>[]>();

  /** Rows for the CURRENT page only — this table never paginates itself. */
  readonly rows = input.required<T[]>();

  /** Shows a loading indicator overlay while true. */
  readonly loading = input<boolean>(false);

  /** Currently active sort field, if any — reflected in the sort-header UI. */
  readonly activeSortField = input<string | null>(null);

  /** Currently active sort direction, if any. */
  readonly activeSortDirection = input<DataTableSortDirection | null>(null);

  /** Emitted whenever the user requests a different sort via a column header. */
  readonly sortChange = output<DataTableSortEvent>();

  get columnKeys(): string[] {
    return this.columns().map((column) => column.key);
  }

  cellValue(column: DataTableColumn<T>, row: T): string {
    if (column.cell) {
      return column.cell(row);
    }
    const value = (row as Record<string, unknown>)[column.key];
    return value === null || value === undefined ? '' : String(value);
  }

  onMatSortChange(sort: Sort): void {
    if (!sort.direction) {
      // Material's sort-header supports a "cleared" third state; LeaseDemo's
      // server-driven tables always keep a deterministic active sort, so a
      // clear is reinterpreted as ascending on the same field rather than
      // silently dropping sort state.
      this.sortChange.emit({ field: sort.active, direction: 'asc' });
      return;
    }
    this.sortChange.emit({ field: sort.active, direction: sort.direction });
  }
}
