package com.leasedemo.lease.application.service;

import com.leasedemo.entity.Customer;
import com.leasedemo.exception.CustomerProfileNotFoundException;
import com.leasedemo.lease.application.dto.CreateLeaseApplicationRequest;
import com.leasedemo.lease.application.dto.LeaseApplicationResponse;
import com.leasedemo.lease.application.entity.LeaseApplication;
import com.leasedemo.lease.application.exception.InvalidApplicationInputException;
import com.leasedemo.lease.application.insurance.InsurancePremiumCalculator;
import com.leasedemo.lease.application.scoring.CreditScoringService;
import com.leasedemo.lease.quote.dto.LeaseQuoteRequest;
import com.leasedemo.lease.quote.dto.LeaseQuoteResponse;
import com.leasedemo.lease.quote.service.LeaseQuoteService;
import com.leasedemo.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Orchestrates lease application submission (M5.3).
 *
 * <p>Flow (M5.3 §16): resolve Customer -> calculate authoritative Lease
 * Quote (may call NBP/Redis) -> calculate/validate insurance -> calculate
 * credit score -> persist the final snapshot via
 * {@link LeaseApplicationPersistenceService} (the ONLY {@code @Transactional}
 * boundary, deliberately not covering the external NBP call).
 */
@Service
public class LeaseApplicationService {

    private final CustomerRepository customerRepository;
    private final LeaseQuoteService leaseQuoteService;
    private final InsurancePremiumCalculator insurancePremiumCalculator;
    private final CreditScoringService creditScoringService;
    private final LeaseApplicationPersistenceService persistenceService;

    public LeaseApplicationService(
            CustomerRepository customerRepository,
            LeaseQuoteService leaseQuoteService,
            InsurancePremiumCalculator insurancePremiumCalculator,
            CreditScoringService creditScoringService,
            LeaseApplicationPersistenceService persistenceService) {
        this.customerRepository = customerRepository;
        this.leaseQuoteService = leaseQuoteService;
        this.insurancePremiumCalculator = insurancePremiumCalculator;
        this.creditScoringService = creditScoringService;
        this.persistenceService = persistenceService;
    }

    public LeaseApplicationResponse submit(String keycloakUserId, CreateLeaseApplicationRequest request) {
        // Trusted security context: Customer identity is derived exclusively
        // from the authenticated JWT subject — never from the request body
        // (M5.3 §5).
        Customer customer = customerRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new CustomerProfileNotFoundException(
                        "No customer profile exists for the authenticated identity"));

        validateFinancialInput(request.monthlyNetIncome(), request.monthlyObligations());

        // Authoritative recalculation — the backend never trusts any
        // monthlyPayment/exchangeRate/financedAmount/totalLeaseCost the
        // browser might have supplied (M5.3 §3). This may call NBP/Redis,
        // so it deliberately happens BEFORE the persistence transaction.
        LeaseQuoteResponse quote = leaseQuoteService.calculate(new LeaseQuoteRequest(
                request.productCode(),
                request.vehiclePrice(),
                request.vehiclePriceCurrency(),
                request.termMonths(),
                request.initialPaymentPercent(),
                request.buyoutPercent(),
                request.leaseType()
        ));

        InsurancePremiumCalculator.Result insurance = insurancePremiumCalculator.calculate(request.insurance());

        CreditScoringService.Result scoring = creditScoringService.score(
                request.monthlyNetIncome(),
                request.monthlyObligations(),
                quote.monthlyPayment(),
                insurance.totalMonthlyPremium());

        LeaseApplication leaseApplication = new LeaseApplication(
                customer,
                quote.productCode(),
                quote.productName(),
                scoring.status(),
                scoring.score(),
                request.monthlyNetIncome(),
                request.monthlyObligations(),
                quote.vehiclePriceOriginal(),
                quote.vehiclePriceCurrency(),
                quote.settlementCurrency(),
                quote.exchangeRate(),
                quote.exchangeRateDate() != null ? LocalDate.parse(quote.exchangeRateDate()) : null,
                quote.vehiclePriceSettlement(),
                quote.termMonths(),
                quote.initialPaymentPercent(),
                quote.initialPayment(),
                quote.buyoutPercent(),
                quote.buyout(),
                quote.leaseType(),
                quote.annualRatePercent(),
                quote.financedAmount(),
                quote.monthlyPayment(),
                quote.totalLeaseCost(),
                quote.estimatedVat(),
                insurance.totalMonthlyPremium(),
                insurance.coverages());

        // The ONLY transactional boundary — persistence only, no external calls.
        LeaseApplication saved = persistenceService.save(leaseApplication);

        BigDecimal estimatedMonthlyTotal = saved.getMonthlyPayment().add(saved.getInsuranceMonthlyPremium());

        return new LeaseApplicationResponse(
                saved.getId(),
                saved.getStatus(),
                saved.getCreditScore(),
                saved.getSubmittedAt(),
                saved.getProductCode(),
                saved.getProductNameSnapshot(),
                saved.getSettlementCurrency(),
                saved.getMonthlyPayment(),
                saved.getInsuranceMonthlyPremium(),
                estimatedMonthlyTotal);
    }

    private void validateFinancialInput(BigDecimal monthlyNetIncome, BigDecimal monthlyObligations) {
        if (monthlyNetIncome == null || monthlyNetIncome.signum() <= 0) {
            throw new InvalidApplicationInputException("monthlyNetIncome must be greater than zero");
        }
        if (monthlyObligations == null || monthlyObligations.signum() < 0) {
            throw new InvalidApplicationInputException("monthlyObligations cannot be negative");
        }
    }
}
