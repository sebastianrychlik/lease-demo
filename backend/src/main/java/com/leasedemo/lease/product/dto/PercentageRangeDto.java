package com.leasedemo.lease.product.dto;

import java.math.BigDecimal;

/** A backend-owned min/max/default/step percentage range rendered by Angular sliders. */
public record PercentageRangeDto(
        BigDecimal minPercent,
        BigDecimal maxPercent,
        BigDecimal defaultPercent,
        BigDecimal stepPercent
) {
}
