package com.luxsoft.dxc.exchange.exception;

/**
 * Application exception thrown when the NBP API is unavailable or returns an unexpected response.
 *
 * <p>Wraps low-level HTTP client errors so they are never propagated directly to callers.
 * Handled by {@link com.luxsoft.dxc.exception.GlobalExceptionHandler}.
 */
public class NbpClientException extends RuntimeException {

    public NbpClientException(String message) {
        super(message);
    }

    public NbpClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
