package com.leasedemo.lease.product.controller;

import com.leasedemo.lease.product.dto.LeaseProductConfigurationResponse;
import com.leasedemo.lease.product.service.LeaseProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller exposing Lease Product configuration (M5.1.2).
 *
 * <p>The current market is resolved entirely server-side via
 * {@code LeaseMarketResolver} — Angular never supplies a market parameter.
 */
@RestController
@RequestMapping("/api/lease-products")
@Tag(name = "Lease Products", description = "Backend-driven Lease Product configuration (M5.1.2)")
@SecurityRequirement(name = "bearerAuth")
public class LeaseProductController {

    private final LeaseProductService leaseProductService;

    public LeaseProductController(LeaseProductService leaseProductService) {
        this.leaseProductService = leaseProductService;
    }

    @Operation(
            summary = "List Lease Products available for the current market",
            description = "Returns enabled, currently valid Lease Product configurations for the "
                    + "market resolved server-side. Angular renders its Reactive Form entirely "
                    + "from this response and never hard-codes business options.")
    @GetMapping("/available")
    public ResponseEntity<List<LeaseProductConfigurationResponse>> getAvailableProducts() {
        return ResponseEntity.ok(leaseProductService.getAvailableProducts());
    }
}
