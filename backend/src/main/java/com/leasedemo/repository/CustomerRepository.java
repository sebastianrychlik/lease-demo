package com.leasedemo.repository;

import com.leasedemo.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    /**
     * Looks up a customer by the deterministic HMAC-SHA-256 hash of their
     * PESEL, without ever needing to decrypt {@code pesel_encrypted}.
     */
    Optional<Customer> findByPeselLookup(String peselLookup);

    boolean existsByPeselLookup(String peselLookup);
}
