package com.leasedemo.lease.application.repository;

import com.leasedemo.lease.application.entity.LeaseApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Persistence access for {@link LeaseApplication} (M5.3).
 *
 * <p>M5.6 (Admin Dashboard) will extend this with list/query methods —
 * kept minimal here per the M5.3 scope guardrails.
 */
public interface LeaseApplicationRepository extends JpaRepository<LeaseApplication, UUID> {
}
