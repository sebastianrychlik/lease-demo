package com.leasedemo.exception;

import com.leasedemo.exchange.exception.NbpClientException;
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
