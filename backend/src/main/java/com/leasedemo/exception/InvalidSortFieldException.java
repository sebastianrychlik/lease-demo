package com.leasedemo.exception;

/**
 * Thrown when a client requests sorting/pagination by a Customer list field
 * that is not on the explicit server-owned sort allow-list
 * (see {@code CustomerService#SUPPORTED_SORT_FIELDS}).
 *
 * <p>The API owns its supported query contract rather than passing
 * arbitrary client-supplied property names into persistence sorting.
 */
public class InvalidSortFieldException extends RuntimeException {

    public InvalidSortFieldException(String message) {
        super(message);
    }
}
