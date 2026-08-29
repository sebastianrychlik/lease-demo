package com.leasedemo.lease.product.repository;

import com.leasedemo.lease.product.entity.LeaseProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeaseProductRepository extends JpaRepository<LeaseProduct, UUID> {

    /**
     * Enabled products for a given market, sorted deterministically by name
     * then code. {@code validFrom}/{@code validTo} are additionally checked
     * in {@code LeaseProductService} since they are date-range, not a
     * simple equality predicate.
     */
    List<LeaseProduct> findByMarketAndEnabledTrueOrderByNameAscCodeAsc(String market);

    Optional<LeaseProduct> findByCode(String code);

    boolean existsByCode(String code);

    /**
     * ALL products, for ADMIN management (M5.1.4) — including disabled,
     * future-valid, and expired ones. Sorted deterministically by market,
     * then name, then code (§7).
     */
    List<LeaseProduct> findAllByOrderByMarketAscNameAscCodeAsc();
}
