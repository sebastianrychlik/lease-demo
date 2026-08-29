package com.leasedemo.lease.quote.controller;

import com.leasedemo.lease.quote.dto.LeaseQuoteRequest;
import com.leasedemo.lease.quote.dto.LeaseQuoteResponse;
import com.leasedemo.lease.quote.service.LeaseQuoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing the Lease Quote Simulator endpoint (M5.1).
 *
 * <p>Stateless: no persistence, no LeaseApplication side effects. The
 * quote is an estimate, not a legally binding financing offer.
 *
 * <p>All operations require a valid Keycloak-issued Bearer JWT
 * ({@code /api/**} is authenticated per {@code SecurityConfig}).
 */
@RestController
@RequestMapping("/api/lease-quotes")
@Tag(name = "Lease Quotes", description = "Stateless lease quote calculation (M5.1)")
@SecurityRequirement(name = "bearerAuth")
public class LeaseQuoteController {

    private final LeaseQuoteService leaseQuoteService;

    public LeaseQuoteController(LeaseQuoteService leaseQuoteService) {
        this.leaseQuoteService = leaseQuoteService;
    }

    @Operation(
            summary = "Calculate a demo lease quote",
            description = "Converts the vehicle price to PLN using the current NBP EUR/PLN rate "
                    + "(when currency is EUR) and runs a deterministic demo amortization "
                    + "calculation with a final buyout/residual payment. This is an ESTIMATE "
                    + "only, not a legally binding financing offer.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quote calculated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "502", description = "NBP exchange rate API unavailable")
    })
    @PostMapping("/calculate")
    public ResponseEntity<LeaseQuoteResponse> calculate(@Valid @RequestBody LeaseQuoteRequest request) {
        return ResponseEntity.ok(leaseQuoteService.calculate(request));
    }
}
