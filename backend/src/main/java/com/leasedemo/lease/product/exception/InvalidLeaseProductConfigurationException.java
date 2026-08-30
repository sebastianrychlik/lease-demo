package com.leasedemo.lease.product.exception;

/**
 * Thrown when an ADMIN create/update Lease Product request fails a
 * cross-field business validation rule (M5.1.4 §16-22) — e.g. settlement
 * currency not among accepted currencies, default term not among the
 * configured terms, an invalid range, or an unknown default lease type.
 *
 * <p>Distinct from bean-validation ({@code @NotNull}/{@code @NotEmpty})
 * failures, which are structural and handled by Spring's own
 * {@code MethodArgumentNotValidException} machinery.
 */
public class InvalidLeaseProductConfigurationException extends RuntimeException {

    public InvalidLeaseProductConfigurationException(String message) {
        super(message);
    }
}
