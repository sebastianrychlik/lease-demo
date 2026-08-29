/**
 * Generic LeaseDemo data-table column definition.
 *
 * `app-data-table` has no knowledge of any specific business domain (e.g.
 * Customer) — feature code supplies column definitions describing how to
 * label/render/sort each column.
 */
export interface DataTableColumn<T> {
  /** Stable column identifier. Used as the sort field name when `sortable`. */
  key: string;

  /** Column header label. */
  label: string;

  /** Whether this column's header supports triggering a sort request. */
  sortable?: boolean;

  /**
   * Renders the cell's display value for a row. Defaults to `String(row[key])`
   * when omitted — supply a function for computed/composed display values
   * (e.g. combining first + last name).
   */
  cell?: (row: T) => string;

  /** Optional extra CSS class applied to both header and data cells. */
  cssClass?: string;
}

export type DataTableSortDirection = 'asc' | 'desc';

export interface DataTableSortEvent {
  field: string;
  direction: DataTableSortDirection;
}
