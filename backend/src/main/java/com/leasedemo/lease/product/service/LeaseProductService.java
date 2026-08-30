package com.leasedemo.lease.product.service;

import com.leasedemo.lease.product.dto.LeaseProductConfigurationResponse;
import com.leasedemo.lease.product.entity.LeaseProduct;
import com.leasedemo.lease.product.exception.InvalidLeaseProductOptionException;
import com.leasedemo.lease.product.exception.LeaseProductNotFoundException;
import com.leasedemo.lease.product.exception.LeaseProductUnavailableException;
import com.leasedemo.lease.product.mapper.LeaseProductMapper;
import com.leasedemo.lease.product.model.LeaseMarket;
import com.leasedemo.lease.product.repository.LeaseProductRepository;
import com.leasedemo.lease.quote.model.LeaseCurrency;
import com.leasedemo.lease.quote.model.LeaseType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Owns Lease Product availability and validation rules.
 *
 * <p>The backend is the sole authority on which products/options exist and
 * whether a submitted quote request is valid for the selected product —
 * Angular's own validators are UX only.
 */
@Service
public class LeaseProductService {

    private final LeaseProductRepository leaseProductRepository;
    private final LeaseProductMapper leaseProductMapper;
    private final LeaseMarketResolver leaseMarketResolver;

    public LeaseProductService(
            LeaseProductRepository leaseProductRepository,
            LeaseProductMapper leaseProductMapper,
            LeaseMarketResolver leaseMarketResolver) {
        this.leaseProductRepository = leaseProductRepository;
        this.leaseProductMapper = leaseProductMapper;
        this.leaseMarketResolver = leaseMarketResolver;
    }

    /**
     * Products available for the current market — enabled, and (if set)
     * currently within {@code validFrom}/{@code validTo}. Sorted
     * deterministically by name, then code.
     */
    @Transactional(readOnly = true)
    public List<LeaseProductConfigurationResponse> getAvailableProducts() {
        LeaseMarket market = leaseMarketResolver.resolveCurrentMarket();
        LocalDate today = LocalDate.now();

        return leaseProductRepository.findByMarketAndEnabledTrueOrderByNameAscCodeAsc(market.name())
                .stream()
                .filter(product -> product.isValidOn(today))
                .map(leaseProductMapper::toConfigurationResponse)
                .toList();
    }

    /**
     * Loads and validates a product by code for the current market. Used by
     * {@code LeaseQuoteService} prior to calculation.
     *
     * @throws LeaseProductNotFoundException    when no product has this code
     * @throws LeaseProductUnavailableException when the product exists but
     *                                          is disabled, belongs to a
     *                                          different market, or is
     *                                          outside its valid date range
     */
    @Transactional(readOnly = true)
    public LeaseProduct loadValidatedProduct(String productCode) {
        LeaseProduct product = leaseProductRepository.findByCode(productCode)
                .orElseThrow(() -> new LeaseProductNotFoundException(
                        "No lease product found for code '" + productCode + "'"));

        LeaseMarket currentMarket = leaseMarketResolver.resolveCurrentMarket();
        if (!product.getMarket().equals(currentMarket.name())) {
            throw new LeaseProductUnavailableException(
                    "Lease product '" + productCode + "' is not available in the current market");
        }
        if (!product.isValidOn(LocalDate.now())) {
            throw new LeaseProductUnavailableException(
                    "Lease product '" + productCode + "' is not currently enabled/available");
        }
        return product;
    }

    /**
     * Validates that the requested quote parameters are all offered by the
     * given (already loaded/validated) product, and returns the annual rate
     * to use for the requested lease type.
     *
     * @throws InvalidLeaseProductOptionException when any requested option
     *                                             is not offered by the
     *                                             product
     */
    public BigDecimal validateAndResolveAnnualRate(
            LeaseProduct product,
            LeaseCurrency currency,
            Integer termMonths,
            BigDecimal initialPaymentPercent,
            BigDecimal buyoutPercent,
            LeaseType leaseType) {

        if (!product.getCurrencies().contains(currency)) {
            throw new InvalidLeaseProductOptionException(
                    "currency '" + currency + "' is not offered by product '" + product.getCode() + "'");
        }
        if (!product.getTermsMonths().contains(termMonths)) {
            throw new InvalidLeaseProductOptionException(
                    "termMonths " + termMonths + " is not offered by product '" + product.getCode() + "'");
        }
        if (isOutOfRange(initialPaymentPercent, product.getInitialPaymentMinPercent(), product.getInitialPaymentMaxPercent())) {
            throw new InvalidLeaseProductOptionException("initialPaymentPercent " + initialPaymentPercent
                    + " is outside the allowed range for product '" + product.getCode() + "'");
        }
        if (isOutOfRange(buyoutPercent, product.getBuyoutMinPercent(), product.getBuyoutMaxPercent())) {
            throw new InvalidLeaseProductOptionException("buyoutPercent " + buyoutPercent
                    + " is outside the allowed range for product '" + product.getCode() + "'");
        }
        BigDecimal annualRatePercent = product.getLeaseTypeAnnualRatePercent().get(leaseType);
        if (annualRatePercent == null) {
            throw new InvalidLeaseProductOptionException(
                    "leaseType " + leaseType + " is not offered by product '" + product.getCode() + "'");
        }
        return annualRatePercent;
    }

    private static boolean isOutOfRange(BigDecimal value, BigDecimal min, BigDecimal max) {
        return value.compareTo(min) < 0 || value.compareTo(max) > 0;
    }
}
