package com.leasedemo.lease.product.exception;

/**
 * Thrown when a submitted lease quote parameter (currency, term, initial
 * payment %, buyout %, or lease type) is not offered/allowed by the
 * selected Lease Product. Angular's own validators are UX only — this is
 * the authoritative business rule enforcement.
 */
public class InvalidLeaseProductOptionException extends RuntimeException {

    public InvalidLeaseProductOptionException(String message) {
        super(message);
    }
}
