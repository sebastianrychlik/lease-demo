package com.leasedemo.controller;

import com.leasedemo.dto.CustomerCreateRequest;
import com.leasedemo.dto.CustomerResponse;
import com.leasedemo.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
                    + "request body, validated, and never persisted or returned in plaintext.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Customer created"),
            @ApiResponse(responseCode = "400", description = "Validation error (e.g. invalid PESEL)"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "409", description = "A customer with this PESEL is already registered")
    })
    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CustomerCreateRequest request) {
        CustomerResponse response = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
