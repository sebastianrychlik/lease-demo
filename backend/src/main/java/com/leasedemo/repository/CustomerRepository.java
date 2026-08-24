package com.leasedemo.repository;

import com.leasedemo.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

/**
 * {@link JpaSpecificationExecutor} is additionally implemented so the
 * Admin Customer list query (M4.4) can compose an optional, case-insensitive
 * free-text search predicate with server-side pagination/sorting — entirely
 * translated into a single SQL query executed by PostgreSQL, without
 * introducing QueryDSL or another persistence dependency.
 */
public interface CustomerRepository extends JpaRepository<Customer, UUID>,
        JpaSpecificationExecutor<Customer> {

    /**
     * Looks up a customer by the deterministic HMAC-SHA-256 hash of their
     * PESEL, without ever needing to decrypt {@code pesel_encrypted}.
     */
    Optional<Customer> findByPeselLookup(String peselLookup);

    boolean existsByPeselLookup(String peselLookup);

    /**
     * Friendly, early duplicate check for an already-registered Keycloak
     * identity. This is NOT the authoritative safeguard — the
     * {@code customers.keycloak_user_id} database {@code UNIQUE} constraint
     * remains the concurrency-safe guarantee against duplicate identities
     * under concurrent requests.
     */
    boolean existsByKeycloakUserId(String keycloakUserId);
}
