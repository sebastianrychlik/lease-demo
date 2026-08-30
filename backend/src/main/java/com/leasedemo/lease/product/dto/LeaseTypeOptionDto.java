package com.leasedemo.lease.product.dto;

import com.leasedemo.lease.quote.model.LeaseType;

import java.math.BigDecimal;

/** A lease type offered by a product, with the APR actually used for calculation. */
public record LeaseTypeOptionDto(
        LeaseType type,
        BigDecimal annualRatePercent
) {
}
