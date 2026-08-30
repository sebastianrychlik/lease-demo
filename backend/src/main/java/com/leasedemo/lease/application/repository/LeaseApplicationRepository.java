package com.leasedemo.lease.application.repository;

import com.leasedemo.lease.application.entity.LeaseApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence access for {@link LeaseApplication} (M5.3).
 *
 * <p>M5.6 (Admin Dashboard) will extend this with list/query methods —
 * kept minimal here per the M5.3 scope guardrails.
 */
public interface LeaseApplicationRepository extends JpaRepository<LeaseApplication, UUID> {

    /**
     * Loads the historical {@code LeaseApplication} snapshot together with
     * its owning {@code Customer} in a single query, so the Kafka
     * consumer (M5.5 §21, §23) can safely read {@code customer.email}
     * after the repository call returns without hitting an uninitialized
     * lazy-loading proxy outside the (already-closed) persistence context.
     */
    @Query("select la from LeaseApplication la join fetch la.customer where la.id = :id")
    Optional<LeaseApplication> findByIdWithCustomer(UUID id);
}
