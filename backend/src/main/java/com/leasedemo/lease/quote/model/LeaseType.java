package com.leasedemo.lease.quote.model;

/**
 * Supported lease types for the M5.1 Lease Quote Simulator.
 *
 * <p>Each lease type has an associated demo nominal annual interest rate,
 * defined in {@code LeaseQuoteService}. Real-world insurance/add-on
 * packages (M5.2) and persistence (M5.3) are out of scope here.
 */
public enum LeaseType {
    OPERATING,
    FINANCIAL
}
