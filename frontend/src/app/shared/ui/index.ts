// =============================================================================
// shared/ui public API
// =============================================================================
//
// Simple barrel for LeaseDemo's reusable UI controls. Import from
// `@shared/ui` (or a relative path to this file) rather than deep-importing
// individual component files, so shared/ui's internal file layout can
// evolve without breaking feature imports.
// =============================================================================

export { ButtonComponent } from './button/button.component';
export type { AppButtonVariant, AppButtonType } from './button/button.component';

export { CardComponent } from './card/card.component';

export { PageHeaderComponent } from './page-header/page-header.component';

export { InputComponent } from './input/input.component';
export type { AppInputType } from './input/input.component';

export { SelectComponent } from './select/select.component';
export type { AppSelectOption } from './select/select.component';

export { CheckboxComponent } from './checkbox/checkbox.component';

export { DataTableComponent } from './data-table/data-table.component';
export type {
  DataTableColumn,
  DataTableSortDirection,
  DataTableSortEvent,
} from './data-table/data-table.model';
