package com.leasedemo.exception;

import com.leasedemo.exchange.exception.NbpClientException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
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

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }
}
