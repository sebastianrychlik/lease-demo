package com.leasedemo.exception;

import com.leasedemo.exchange.exception.NbpClientException;
import com.leasedemo.lease.product.exception.DuplicateLeaseProductCodeException;
import com.leasedemo.lease.product.exception.InvalidLeaseProductConfigurationException;
import com.leasedemo.lease.product.exception.InvalidLeaseProductOptionException;
import com.leasedemo.lease.product.exception.LeaseProductNotFoundException;
import com.leasedemo.lease.product.exception.LeaseProductUnavailableException;
import com.leasedemo.lease.quote.exception.UnsupportedCurrencyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Handles NBP API integration failures.
     *
     * <p>Returns HTTP 502 Bad Gateway when the upstream NBP API is unavailable
     * or returns an unexpected response. Low-level client exceptions are never
     * exposed directly to the caller.
     */
    @ExceptionHandler(NbpClientException.class)
    public ProblemDetail handleNbpClientException(NbpClientException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
        problemDetail.setTitle("NBP API Unavailable");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    /**
     * Handles lease quote requests for a currency not present in the
     * current NBP Table A response (M5.1).
     */
    @ExceptionHandler(UnsupportedCurrencyException.class)
    public ProblemDetail handleUnsupportedCurrencyException(UnsupportedCurrencyException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
        problemDetail.setTitle("Unsupported Currency");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    /**
     * Handles lease quote requests referencing an unknown Lease Product
     * code (M5.1.2).
     */
    @ExceptionHandler(LeaseProductNotFoundException.class)
    public ProblemDetail handleLeaseProductNotFoundException(LeaseProductNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle("Lease Product Not Found");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    /**
     * Handles lease quote requests for a Lease Product that exists but is
     * disabled, belongs to a different market, or is outside its valid
     * date range (M5.1.2).
     */
    @ExceptionHandler(LeaseProductUnavailableException.class)
    public ProblemDetail handleLeaseProductUnavailableException(LeaseProductUnavailableException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setTitle("Lease Product Unavailable");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    /**
     * Handles lease quote requests whose currency/term/percent range/lease
     * type is not offered by the selected Lease Product (M5.1.2). Angular's
     * own form validators are UX only — this is the authoritative check.
     */
    @ExceptionHandler(InvalidLeaseProductOptionException.class)
    public ProblemDetail handleInvalidLeaseProductOptionException(InvalidLeaseProductOptionException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Invalid Lease Product Option");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    /**
     * Handles ADMIN Lease Product create requests using a code that
     * already exists (M5.1.4 §11) — product code must be unique.
     */
    @ExceptionHandler(DuplicateLeaseProductCodeException.class)
    public ProblemDetail handleDuplicateLeaseProductCodeException(DuplicateLeaseProductCodeException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setTitle("Duplicate Lease Product Code");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    /**
     * Handles ADMIN Lease Product create/update requests that fail
     * cross-field business validation (M5.1.4 §16-22) — e.g. settlement
     * currency not among accepted currencies, invalid range, unknown
     * default lease type.
     */
    @ExceptionHandler(InvalidLeaseProductConfigurationException.class)
    public ProblemDetail handleInvalidLeaseProductConfigurationException(InvalidLeaseProductConfigurationException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Invalid Lease Product Configuration");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    /**
     * Handles PESEL validation failures (bad format, checksum, or a
     * DOB/gender mismatch against the declared values).
     */
    @ExceptionHandler(InvalidPeselException.class)
    public ProblemDetail handleInvalidPeselException(InvalidPeselException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Invalid PESEL");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    /**
     * Handles attempts to register a customer whose PESEL is already on file.
     */
    @ExceptionHandler(DuplicateCustomerException.class)
    public ProblemDetail handleDuplicateCustomerException(DuplicateCustomerException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setTitle("Duplicate Customer");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    /**
     * Handles a self-service {@code GET /api/customers/me} request for an
     * authenticated JWT subject with no associated Customer domain profile
     * yet (M4.5). Signals to Angular that onboarding should start.
     */
    @ExceptionHandler(CustomerProfileNotFoundException.class)
    public ProblemDetail handleCustomerProfileNotFoundException(CustomerProfileNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle("Customer Profile Not Found");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    /**
     * Handles an Admin Customer list request for a sort field outside the
     * server-owned allow-list (M4.4).
     */
    @ExceptionHandler(InvalidSortFieldException.class)
    public ProblemDetail handleInvalidSortFieldException(InvalidSortFieldException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Invalid Sort Field");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }

    /**
     * Handles method-security ({@code @PreAuthorize}) denials, e.g. an
     * authenticated ROLE_ADMIN-only identity calling {@code GET
     * /api/customers/me} (M4.5, requires ROLE_CUSTOMER). Without this
     * explicit handler, the generic {@link Exception} handler below would
     * intercept it first and return 500 instead of the correct 403.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDeniedException(AccessDeniedException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problemDetail.setTitle("Access Denied");
        problemDetail.setDetail("You do not have permission to access this resource");
        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }
}
