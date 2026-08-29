package com.leasedemo.lease.quote.service;

import com.leasedemo.lease.quote.dto.LeaseQuoteRequest;
import com.leasedemo.lease.quote.dto.LeaseQuoteResponse;
import com.leasedemo.lease.quote.model.LeaseCurrency;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Orchestrates the M5.1 lease quote calculation.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Resolve the PLN exchange rate via {@link LeaseQuoteExchangeRateResolver}
 *       (which reuses the existing NBP integration).</li>
 *   <li>Convert the vehicle price to PLN.</li>
 *   <li>Delegate the deterministic financial calculation to
 *       {@link LeaseQuoteCalculator}.</li>
 *   <li>Assemble the typed {@link LeaseQuoteResponse}.</li>
 * </ul>
 *
 * <p>This class must not contain HTTP/controller concerns or raw NBP
 * client calls — those belong to {@link LeaseQuoteExchangeRateResolver}.
 */
@Service
public class LeaseQuoteService {

    private final LeaseQuoteExchangeRateResolver exchangeRateResolver;
    private final LeaseQuoteCalculator calculator;

    public LeaseQuoteService(
            LeaseQuoteExchangeRateResolver exchangeRateResolver,
            LeaseQuoteCalculator calculator
    ) {
        this.exchangeRateResolver = exchangeRateResolver;
        this.calculator = calculator;
    }

    public LeaseQuoteResponse calculate(LeaseQuoteRequest request) {
        LeaseCurrency currency = request.currency();
        LeaseQuoteExchangeRateResolver.ResolvedRate resolvedRate = exchangeRateResolver.resolve(currency);

        BigDecimal vehiclePricePln = request.vehiclePrice()
                .multiply(resolvedRate.rate())
                .setScale(2, RoundingMode.HALF_UP);

        LeaseQuoteCalculator.Result result = calculator.calculate(
                vehiclePricePln,
                request.initialPaymentPercent(),
                request.buyoutPercent(),
                request.termMonths(),
                request.leaseType()
        );

        return new LeaseQuoteResponse(
                request.vehiclePrice(),
                currency,
                resolvedRate.rate(),
                resolvedRate.effectiveDate(),
                result.vehiclePricePln(),
                request.termMonths(),
                request.initialPaymentPercent(),
                result.initialPaymentPln(),
                request.buyoutPercent(),
                result.buyoutPln(),
                request.leaseType(),
                calculator.annualRatePercent(request.leaseType()),
                result.financedAmountPln(),
                result.monthlyPaymentPln(),
                result.totalLeaseCostPln(),
                result.estimatedVatPln()
        );
    }
}
