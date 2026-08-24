package com.leasedemo.repository;

import com.leasedemo.entity.Customer;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Free-text search predicate for the Admin Customer list query (M4.4).
 *
 * <p>Matches case-insensitively against non-sensitive display fields only:
 * {@code firstName}, {@code lastName}, {@code email}. PESEL (raw, encrypted,
 * or its lookup hash) is intentionally never part of this search — the
 * encrypted PESEL is not searchable without decrypting every row, which
 * this milestone explicitly does not do.
 */
public final class CustomerSpecifications {

    private CustomerSpecifications() {
    }

    /**
     * Builds a specification matching customers whose first name, last
     * name, or email contains {@code searchTerm} (case-insensitive).
     * Returns a specification matching everything when {@code searchTerm}
     * is null/blank.
     */
    public static Specification<Customer> searchByNameOrEmail(String searchTerm) {
        if (!StringUtils.hasText(searchTerm)) {
            return (root, query, cb) -> cb.conjunction();
        }
        String likePattern = "%" + searchTerm.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("firstName")), likePattern),
                cb.like(cb.lower(root.get("lastName")), likePattern),
                cb.like(cb.lower(root.get("email")), likePattern));
    }
}
