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
}
