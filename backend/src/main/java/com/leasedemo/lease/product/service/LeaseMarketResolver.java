package com.leasedemo.lease.product.service;

import com.leasedemo.lease.product.model.LeaseMarket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Resolves the current business market for Lease Product availability and
 * quote validation.
 *
 * <p>The frontend must NOT supply the market (e.g. {@code ?market=PL}) —
 * the backend is the sole authority. For M5.1.2 the market is resolved
 * from application configuration ({@code leasedemo.lease.default-market}).
 *
 * <p>This is a deliberately small, explicit abstraction: later it can be
 * changed to resolve the market from the authenticated Customer
 * ({@code customer.market}) instead, without changing any Lease Product
 * API contract.
 */
@Component
public class LeaseMarketResolver {

    private final LeaseMarket defaultMarket;

    public LeaseMarketResolver(@Value("${leasedemo.lease.default-market:PL}") LeaseMarket defaultMarket) {
        this.defaultMarket = defaultMarket;
    }

    /** Resolves the current market. Ignores any client-supplied input by design. */
    public LeaseMarket resolveCurrentMarket() {
        return defaultMarket;
    }
}
