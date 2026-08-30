package com.leasedemo.lease.quote.model;

/**
 * Currencies supported by the M5.1 Lease Quote Simulator.
 *
 * <p>{@link #PLN} always uses an exchange rate of {@code 1}. {@link #EUR}
 * is converted to PLN using the current NBP Table A EUR/PLN mid rate via
 * the existing {@code com.leasedemo.exchange} integration.
 */
public enum LeaseCurrency {
    PLN,
    EUR
}
