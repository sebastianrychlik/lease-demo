package com.leasedemo.lease.quote.service;

import com.leasedemo.lease.product.entity.LeaseProduct;
import com.leasedemo.lease.product.service.LeaseProductService;
import com.leasedemo.lease.quote.dto.LeaseQuoteRequest;
import com.leasedemo.lease.quote.dto.LeaseQuoteResponse;
import com.leasedemo.lease.quote.model.LeaseCurrency;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Orchestrates the M5.1 / M5.1.2 / M5.1.3.2 lease quote calculation.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Load and validate the selected {@link LeaseProduct} via
 *       {@link LeaseProductService} — the backend, never Angular, is the
 *       authority on whether the requested options are valid.</li>
 *   <li>Read the product's {@code settlementCurrency} — never accepted
 *       from the client (M5.1.3.2 §9).</li>
 *   <li>Resolve the exchange rate from the request's
 *       {@code vehiclePriceCurrency} to the product's
 *       {@code settlementCurrency} via {@link LeaseQuoteExchangeRateResolver}
 *       (which reuses the existing NBP integration).</li>
 *   <li>Convert the vehicle price into the settlement currency.</li>
 *   <li>Delegate the deterministic financial calculation to
 *       {@link LeaseQuoteCalculator}, passing in the product-resolved APR.</li>
 *   <li>Assemble the typed {@link LeaseQuoteResponse}.</li>
 * </ul>
 *
 * <p>This class must not contain HTTP/controller concerns or raw NBP
 * client calls — those belong to {@link LeaseQuoteExchangeRateResolver}.
 */
@Service
public class LeaseQuoteService {

    private final LeaseProductService leaseProductService;
    private final LeaseQuoteExchangeRateResolver exchangeRateResolver;
    private final LeaseQuoteCalculator calculator;

    public LeaseQuoteService(
            LeaseProductService leaseProductService,
            LeaseQuoteExchangeRateResolver exchangeRateResolver,
            LeaseQuoteCalculator calculator
    ) {
        this.leaseProductService = leaseProductService;
        this.exchangeRateResolver = exchangeRateResolver;
        this.calculator = calculator;
    }

    public LeaseQuoteResponse calculate(LeaseQuoteRequest request) {
        LeaseProduct product = leaseProductService.loadValidatedProduct(request.productCode());

        BigDecimal annualRatePercent = leaseProductService.validateAndResolveAnnualRate(
                product,
                request.vehiclePriceCurrency(),
                request.termMonths(),
                request.initialPaymentPercent(),
                request.buyoutPercent(),
                request.leaseType()
        );

        LeaseCurrency vehiclePriceCurrency = request.vehiclePriceCurrency();
        LeaseCurrency settlementCurrency = product.getSettlementCurrency();
        LeaseQuoteExchangeRateResolver.ResolvedRate resolvedRate =
                exchangeRateResolver.resolve(vehiclePriceCurrency, settlementCurrency);

        BigDecimal vehiclePriceSettlement = request.vehiclePrice()
                .multiply(resolvedRate.rate())
                .setScale(2, RoundingMode.HALF_UP);

        LeaseQuoteCalculator.Result result = calculator.calculate(
                vehiclePriceSettlement,
                request.initialPaymentPercent(),
                request.buyoutPercent(),
                request.termMonths(),
                annualRatePercent
        );

        return new LeaseQuoteResponse(
                product.getCode(),
                product.getName(),
                request.vehiclePrice(),
                vehiclePriceCurrency,
                settlementCurrency,
                resolvedRate.rate(),
                resolvedRate.effectiveDate(),
                result.vehiclePrice(),
                request.termMonths(),
                request.initialPaymentPercent(),
                result.initialPayment(),
                request.buyoutPercent(),
                result.buyout(),
                request.leaseType(),
                annualRatePercent,
                result.financedAmount(),
                result.monthlyPayment(),
                result.totalLeaseCost(),
                result.estimatedVat()
        );
    }
}
