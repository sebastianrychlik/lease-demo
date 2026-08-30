package com.leasedemo.lease.product.mapper;

import com.leasedemo.lease.product.dto.AdminLeaseProductResponse;
import com.leasedemo.lease.product.dto.LeaseProductConfigurationResponse;
import com.leasedemo.lease.product.dto.LeaseTypeOptionDto;
import com.leasedemo.lease.product.dto.PercentageRangeDto;
import com.leasedemo.lease.product.entity.LeaseProduct;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Maps the {@link LeaseProduct} JPA entity to its outbound
 * {@link LeaseProductConfigurationResponse} DTO. The entity is never
 * returned directly from the controller.
 */
@Component
public class LeaseProductMapper {

    public LeaseProductConfigurationResponse toConfigurationResponse(LeaseProduct product) {
        return new LeaseProductConfigurationResponse(
                product.getCode(),
                product.getName(),
                product.getMarket(),
                product.getCurrencies().stream()
                        .sorted(Comparator.comparing(Enum::name))
                        .toList(),
                product.getSettlementCurrency(),
                product.getTermsMonths().stream()
                        .sorted()
                        .toList(),
                new PercentageRangeDto(
                        product.getInitialPaymentMinPercent(),
                        product.getInitialPaymentMaxPercent(),
                        product.getInitialPaymentDefaultPercent(),
                        product.getInitialPaymentStepPercent()
                ),
                new PercentageRangeDto(
                        product.getBuyoutMinPercent(),
                        product.getBuyoutMaxPercent(),
                        product.getBuyoutDefaultPercent(),
                        product.getBuyoutStepPercent()
                ),
                toLeaseTypeOptions(product.getLeaseTypeAnnualRatePercent()),
                product.getDefaultCurrency(),
                product.getDefaultTermMonths(),
                product.getDefaultLeaseType()
        );
    }

    /**
     * Maps the entity to the ADMIN-facing {@link AdminLeaseProductResponse}
     * (M5.1.4) — includes {@code enabled}, validity dates, and audit
     * timestamps that the CUSTOMER DTO deliberately omits.
     */
    public AdminLeaseProductResponse toAdminResponse(LeaseProduct product) {
        return new AdminLeaseProductResponse(
                product.getCode(),
                product.getName(),
                product.getMarket(),
                product.isEnabled(),
                product.getValidFrom(),
                product.getValidTo(),
                product.getCurrencies().stream()
                        .sorted(Comparator.comparing(Enum::name))
                        .toList(),
                product.getSettlementCurrency(),
                product.getDefaultCurrency(),
                product.getTermsMonths().stream()
                        .sorted()
                        .toList(),
                product.getDefaultTermMonths(),
                new PercentageRangeDto(
                        product.getInitialPaymentMinPercent(),
                        product.getInitialPaymentMaxPercent(),
                        product.getInitialPaymentDefaultPercent(),
                        product.getInitialPaymentStepPercent()
                ),
                new PercentageRangeDto(
                        product.getBuyoutMinPercent(),
                        product.getBuyoutMaxPercent(),
                        product.getBuyoutDefaultPercent(),
                        product.getBuyoutStepPercent()
                ),
                toLeaseTypeOptions(product.getLeaseTypeAnnualRatePercent()),
                product.getDefaultLeaseType(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    private List<LeaseTypeOptionDto> toLeaseTypeOptions(
            Map<com.leasedemo.lease.quote.model.LeaseType, java.math.BigDecimal> rates) {
        return rates.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().name()))
                .map(entry -> new LeaseTypeOptionDto(entry.getKey(), entry.getValue()))
                .toList();
    }
}
