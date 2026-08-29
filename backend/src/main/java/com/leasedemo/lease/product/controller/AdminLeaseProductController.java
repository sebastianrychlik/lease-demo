package com.leasedemo.lease.product.controller;

import com.leasedemo.lease.product.dto.AdminLeaseProductResponse;
import com.leasedemo.lease.product.dto.CreateLeaseProductRequest;
import com.leasedemo.lease.product.dto.UpdateLeaseProductRequest;
import com.leasedemo.lease.product.service.AdminLeaseProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ADMIN REST controller for Lease Product configuration management
 * (M5.1.4).
 *
 * <p>Distinct namespace ({@code /api/admin/lease-products}) from the
 * CUSTOMER-facing {@code /api/lease-products/available} — writes never go
 * through the CUSTOMER endpoint. Every method additionally requires
 * ROLE_ADMIN via {@code @PreAuthorize}, on top of the URL-pattern
 * enforcement in {@code SecurityConfig} — Angular's hidden navigation is
 * NOT the trust boundary.
 *
 * <p>No physical DELETE endpoint exists — products are deactivated via
 * {@code enabled = false} through the update endpoint (§12).
 */
@RestController
@RequestMapping("/api/admin/lease-products")
@Tag(name = "Admin Lease Products", description = "ADMIN Lease Product configuration management (M5.1.4)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminLeaseProductController {

    private final AdminLeaseProductService adminLeaseProductService;

    public AdminLeaseProductController(AdminLeaseProductService adminLeaseProductService) {
        this.adminLeaseProductService = adminLeaseProductService;
    }

    @Operation(summary = "List ALL Lease Products for administration",
            description = "Includes enabled, disabled, currently-valid, future-valid, and expired products.")
    @GetMapping
    public ResponseEntity<List<AdminLeaseProductResponse>> getAllProducts() {
        return ResponseEntity.ok(adminLeaseProductService.getAllProducts());
    }

    @Operation(summary = "Get a Lease Product's complete editable configuration")
    @GetMapping("/{code}")
    public ResponseEntity<AdminLeaseProductResponse> getProduct(@PathVariable String code) {
        return ResponseEntity.ok(adminLeaseProductService.getProduct(code));
    }

    @Operation(summary = "Create a new Lease Product")
    @PostMapping
    public ResponseEntity<AdminLeaseProductResponse> createProduct(
            @Valid @RequestBody CreateLeaseProductRequest request) {
        AdminLeaseProductResponse created = adminLeaseProductService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Update a Lease Product's configuration",
            description = "Product code is stable identity and is not part of this request body.")
    @PutMapping("/{code}")
    public ResponseEntity<AdminLeaseProductResponse> updateProduct(
            @PathVariable String code,
            @Valid @RequestBody UpdateLeaseProductRequest request) {
        return ResponseEntity.ok(adminLeaseProductService.updateProduct(code, request));
    }
}
