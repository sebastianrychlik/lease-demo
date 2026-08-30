package com.leasedemo.lease.application.service;

import com.leasedemo.lease.application.entity.LeaseApplication;
import com.leasedemo.lease.application.repository.LeaseApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tiny persistence boundary for {@link LeaseApplication} (M5.3 §16).
 *
 * <p>Kept as its own bean so the {@code @Transactional} boundary covers
 * ONLY the database write — never the NBP/Redis external call performed
 * earlier in {@code LeaseApplicationService}'s orchestration.
 */
@Service
public class LeaseApplicationPersistenceService {

    private final LeaseApplicationRepository leaseApplicationRepository;

    public LeaseApplicationPersistenceService(LeaseApplicationRepository leaseApplicationRepository) {
        this.leaseApplicationRepository = leaseApplicationRepository;
    }

    @Transactional
    public LeaseApplication save(LeaseApplication leaseApplication) {
        return leaseApplicationRepository.save(leaseApplication);
    }
}
