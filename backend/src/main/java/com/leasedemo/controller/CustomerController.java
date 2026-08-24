package com.leasedemo.controller;

import com.leasedemo.dto.CustomerCreateRequest;
import com.leasedemo.dto.CustomerListItemResponse;
import com.leasedemo.dto.CustomerProfileResponse;
import com.leasedemo.dto.CustomerResponse;
import com.leasedemo.dto.PageResponse;
import com.leasedemo.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Customer registration API.
 *
 * <p>All operations require a valid Keycloak-issued Bearer JWT
 * ({@code /api/**} is authenticated per {@code SecurityConfig}); documented
 * here via the {@code bearerAuth} OpenAPI security scheme (M4.1.1).
 */
@RestController
@RequestMapping("/api/customers")
@Tag(name = "Customers", description = "Customer registration")
@SecurityRequirement(name = "bearerAuth")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @Operation(
            summary = "Register a new customer",
            description = "Creates a customer record. The PESEL is accepted only in the "
                    + "request body, validated, and never persisted or returned in plaintext. "
                    + "The Customer identity is bound to the authenticated JWT subject and "
                    + "cannot be supplied by the request body.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Customer created"),
            @ApiResponse(responseCode = "400", description = "Validation error (e.g. invalid PESEL)"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "409", description = "A customer with this PESEL or "
                    + "authenticated identity is already registered")
    })
    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CustomerCreateRequest request) {
        // Trusted security context: Spring Security has already validated this JWT.
        // The Keycloak subject is never taken from client-supplied request data.
        String keycloakUserId = jwt.getSubject();
        CustomerResponse response = customerService.createCustomer(keycloakUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Get the authenticated customer's own profile",
            description = "Resolves the Customer domain profile owned by the authenticated JWT "
                    + "subject (sub claim). The browser never supplies a customerId, "
                    + "keycloakUserId, or email to select which profile is returned. "
                    + "PESEL (raw, encrypted, or lookup hash) and keycloakUserId are never "
                    + "included in the response. A 404 response means the authenticated "
                    + "identity has not yet completed onboarding — it is an expected state, "
                    + "not a server error.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer profile found"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Authenticated but not ROLE_CUSTOMER"),
            @ApiResponse(responseCode = "404", description = "Authenticated identity has no "
                    + "Customer profile yet — onboarding required")
    })
    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CustomerProfileResponse> getCurrentCustomerProfile(@AuthenticationPrincipal Jwt jwt) {
        // Trusted security context: identity is derived exclusively from the
        // authenticated JWT subject, never from any client-supplied value.
        String keycloakUserId = jwt.getSubject();
        CustomerProfileResponse response = customerService.getCurrentCustomerProfile(keycloakUserId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "List customers (Admin only)",
            description = "Server-side paged, sorted, and optionally searched Customer list. "
                    + "Authorization is enforced authoritatively by Spring Security "
                    + "(ROLE_ADMIN) — see SecurityConfig — not by this controller. "
                    + "PESEL (raw, encrypted, or lookup hash) is never returned.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paged customer list"),
            @ApiResponse(responseCode = "400", description = "Unsupported sort field"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Authenticated but not ROLE_ADMIN")
    })
    @GetMapping
    public PageResponse<CustomerListItemResponse> getCustomers(
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Free-text search over first name / last name / email")
            @RequestParam(required = false) String search,
            @Parameter(description = "Sort field: firstName, lastName, email, dateOfBirth, createdAt")
            @RequestParam(required = false) String sortField,
            @Parameter(description = "Sort direction: asc or desc") @RequestParam(required = false) String sortDirection) {
        return customerService.getCustomers(page, size, search, sortField, sortDirection);
    }
}
