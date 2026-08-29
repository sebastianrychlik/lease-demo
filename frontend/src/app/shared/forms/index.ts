// =============================================================================
// shared/forms public API
// =============================================================================
//
// Reusable Reactive-Forms validation primitives and the central validation
// message resolver (M5.1.4). Feature forms compose these with their own
// field names/business rules rather than duplicating validator lambdas or
// hard-coding PL/EN strings.
// =============================================================================

export {
  nonNegativeValidator,
  positiveValidator,
  maxValueValidator,
  integerRangeValidator,
  membershipValidator,
  duplicateValidator,
  rangeOrderValidator,
  defaultOutsideRangeValidator,
  dateOrderValidator,
  atLeastOneEnabledValidator,
  selectedInEnabledGroupValidator,
} from './validators/ld-validators';

export { resolveValidationMessage } from './validation-message';
