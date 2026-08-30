package com.leasedemo.lease.application.controller;

import com.leasedemo.lease.application.dto.CreateLeaseApplicationRequest;
import com.leasedemo.lease.application.dto.LeaseApplicationResponse;
import com.leasedemo.lease.application.service.LeaseApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing Lease Application submission (M5.3).
 *
 * <p>All operations require a valid Keycloak-issued Bearer JWT with the
 * CUSTOMER role — Customer identity is derived exclusively from the JWT
 * {@code sub} claim, never from the request body (M5.3 §5, §18).
 */
@RestController
@RequestMapping("/api/lease-applications")
@Tag(name = "Lease Applications", description = "Lease application persistence + credit scoring (M5.3)")
@SecurityRequirement(name = "bearerAuth")
public class LeaseApplicationController {

    private final LeaseApplicationService leaseApplicationService;

    public LeaseApplicationController(LeaseApplicationService leaseApplicationService) {
        this.leaseApplicationService = leaseApplicationService;
    }

    @Operation(
            summary = "Submit a lease application",
            description = "Recalculates the authoritative Lease Quote and insurance premium "
                    + "server-side, computes a deterministic demo credit score, and persists "
                    + "the resulting LeaseApplication snapshot. The frontend never supplies "
                    + "monetary quote values, insurance premiums, credit score, or decision.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Application submitted and persisted"),
            @ApiResponse(responseCode = "400", description = "Invalid input (financial data, insurance configuration, or quote parameters)"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Authenticated but not ROLE_CUSTOMER"),
            @ApiResponse(responseCode = "404", description = "Authenticated identity has no Customer profile yet"),
            @ApiResponse(responseCode = "502", description = "NBP exchange rate API unavailable")
    })
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<LeaseApplicationResponse> submit(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateLeaseApplicationRequest request) {
        String keycloakUserId = jwt.getSubject();
        LeaseApplicationResponse response = leaseApplicationService.submit(keycloakUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
