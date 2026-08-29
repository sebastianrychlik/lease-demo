package com.leasedemo.lease.product.service;

import com.leasedemo.lease.product.dto.AdminLeaseProductResponse;
import com.leasedemo.lease.product.dto.CreateLeaseProductRequest;
import com.leasedemo.lease.product.dto.LeaseTypeOptionDto;
import com.leasedemo.lease.product.dto.PercentageRangeDto;
import com.leasedemo.lease.product.dto.UpdateLeaseProductRequest;
import com.leasedemo.lease.product.entity.LeaseProduct;
import com.leasedemo.lease.product.exception.DuplicateLeaseProductCodeException;
import com.leasedemo.lease.product.exception.InvalidLeaseProductConfigurationException;
import com.leasedemo.lease.product.exception.LeaseProductNotFoundException;
import com.leasedemo.lease.product.mapper.LeaseProductMapper;
import com.leasedemo.lease.product.model.LeaseMarket;
import com.leasedemo.lease.product.repository.LeaseProductRepository;
import com.leasedemo.lease.quote.model.LeaseCurrency;
import com.leasedemo.lease.quote.model.LeaseType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Owns ADMIN write operations (create/update/list/detail) for Lease
 * Products (M5.1.4).
 *
 * <p>Kept separate from {@link LeaseProductService} (CUSTOMER
 * availability/validation logic) so each service stays focused (§33).
 * Business validation here is authoritative — the Angular ADMIN form's own
 * validators are UX only.
 *
 * <p>Every create/update runs inside ONE {@code @Transactional} write:
 * {@code lease_product} plus its three child collection tables
 * (currencies, terms, lease types) are all persisted/updated together, so a
 * mid-write validation failure can never leave a half-updated product
 * (§14-15).
 */
@Service
public class AdminLeaseProductService {

    private static final int MIN_TERM_MONTHS = 6;
    private static final int MAX_TERM_MONTHS = 120;
    private static final BigDecimal MAX_ANNUAL_RATE_PERCENT = BigDecimal.valueOf(100);

    private final LeaseProductRepository leaseProductRepository;
    private final LeaseProductMapper leaseProductMapper;

    public AdminLeaseProductService(
            LeaseProductRepository leaseProductRepository,
            LeaseProductMapper leaseProductMapper) {
        this.leaseProductRepository = leaseProductRepository;
        this.leaseProductMapper = leaseProductMapper;
    }

    /** All products — enabled/disabled/valid/expired — for ADMIN management (§7). */
    @Transactional(readOnly = true)
    public List<AdminLeaseProductResponse> getAllProducts() {
        return leaseProductRepository.findAllByOrderByMarketAscNameAscCodeAsc().stream()
                .map(leaseProductMapper::toAdminResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminLeaseProductResponse getProduct(String code) {
        return leaseProductMapper.toAdminResponse(loadByCode(code));
    }

    @Transactional
    public AdminLeaseProductResponse createProduct(CreateLeaseProductRequest request) {
        if (leaseProductRepository.existsByCode(request.code())) {
            throw new DuplicateLeaseProductCodeException(
                    "A lease product with code '" + request.code() + "' already exists");
        }

        LeaseMarket market = parseMarket(request.market());
        Set<LeaseCurrency> currencies = new LinkedHashSet<>(request.currencies());
        Set<Integer> terms = new LinkedHashSet<>(request.terms());
        Map<LeaseType, BigDecimal> leaseTypeRates = toRateMap(request.leaseTypes());

        validateConfiguration(
                currencies, request.settlementCurrency(), request.defaultCurrency(),
                terms, request.defaultTermMonths(),
                request.initialPayment(), request.buyout(),
                leaseTypeRates, request.defaultLeaseType(),
                request.validFrom(), request.validTo());

        LeaseProduct product = new LeaseProduct();
        product.setCode(request.code());
        product.setMarket(market.name());
        applyConfiguration(
                product, request.name(), request.enabled(), request.validFrom(), request.validTo(),
                currencies, request.settlementCurrency(), request.defaultCurrency(),
                terms, request.defaultTermMonths(),
                request.initialPayment(), request.buyout(),
                leaseTypeRates, request.defaultLeaseType());

        LeaseProduct saved = leaseProductRepository.save(product);
        return leaseProductMapper.toAdminResponse(saved);
    }

    @Transactional
    public AdminLeaseProductResponse updateProduct(String code, UpdateLeaseProductRequest request) {
        LeaseProduct product = loadByCode(code);

        LeaseMarket market = parseMarket(request.market());
        Set<LeaseCurrency> currencies = new LinkedHashSet<>(request.currencies());
        Set<Integer> terms = new LinkedHashSet<>(request.terms());
        Map<LeaseType, BigDecimal> leaseTypeRates = toRateMap(request.leaseTypes());

        validateConfiguration(
                currencies, request.settlementCurrency(), request.defaultCurrency(),
                terms, request.defaultTermMonths(),
                request.initialPayment(), request.buyout(),
                leaseTypeRates, request.defaultLeaseType(),
                request.validFrom(), request.validTo());

        product.setMarket(market.name());
        applyConfiguration(
                product, request.name(), request.enabled(), request.validFrom(), request.validTo(),
                currencies, request.settlementCurrency(), request.defaultCurrency(),
                terms, request.defaultTermMonths(),
                request.initialPayment(), request.buyout(),
                leaseTypeRates, request.defaultLeaseType());

        // product code is stable identity — never reassigned here (§10-11).
        LeaseProduct saved = leaseProductRepository.save(product);
        return leaseProductMapper.toAdminResponse(saved);
    }

    private LeaseProduct loadByCode(String code) {
        return leaseProductRepository.findByCode(code)
                .orElseThrow(() -> new LeaseProductNotFoundException(
                        "No lease product found for code '" + code + "'"));
    }

    private void applyConfiguration(
            LeaseProduct product,
            String name,
            boolean enabled,
            LocalDate validFrom,
            LocalDate validTo,
            Set<LeaseCurrency> currencies,
            LeaseCurrency settlementCurrency,
            LeaseCurrency defaultCurrency,
            Set<Integer> terms,
            Integer defaultTermMonths,
            PercentageRangeDto initialPayment,
            PercentageRangeDto buyout,
            Map<LeaseType, BigDecimal> leaseTypeRates,
            LeaseType defaultLeaseType) {

        product.setName(name);
        product.setEnabled(enabled);
        product.setValidFrom(validFrom);
        product.setValidTo(validTo);

        product.replaceCurrencies(currencies);
        product.setSettlementCurrency(settlementCurrency);
        product.setDefaultCurrency(defaultCurrency);

        product.replaceTermsMonths(terms);
        product.setDefaultTermMonths(defaultTermMonths);

        product.setInitialPaymentMinPercent(initialPayment.minPercent());
        product.setInitialPaymentMaxPercent(initialPayment.maxPercent());
        product.setInitialPaymentDefaultPercent(initialPayment.defaultPercent());
        product.setInitialPaymentStepPercent(initialPayment.stepPercent());

        product.setBuyoutMinPercent(buyout.minPercent());
        product.setBuyoutMaxPercent(buyout.maxPercent());
        product.setBuyoutDefaultPercent(buyout.defaultPercent());
        product.setBuyoutStepPercent(buyout.stepPercent());

        product.replaceLeaseTypeAnnualRatePercent(leaseTypeRates);
        product.setDefaultLeaseType(defaultLeaseType);
    }

    private Map<LeaseType, BigDecimal> toRateMap(List<LeaseTypeOptionDto> leaseTypes) {
        Map<LeaseType, BigDecimal> rates = new EnumMap<>(LeaseType.class);
        for (LeaseTypeOptionDto option : leaseTypes) {
            if (rates.containsKey(option.type())) {
                throw new InvalidLeaseProductConfigurationException(
                        "leaseType '" + option.type() + "' is configured more than once");
            }
            rates.put(option.type(), option.annualRatePercent());
        }
        return rates;
    }

    private LeaseMarket parseMarket(String market) {
        try {
            return LeaseMarket.valueOf(market);
        } catch (IllegalArgumentException ex) {
            throw new InvalidLeaseProductConfigurationException("Unknown market '" + market + "'");
        }
    }

    /**
     * Authoritative cross-field business validation (§16-22). Bean
     * validation ({@code @NotNull}/{@code @NotEmpty}) on the request DTOs
     * has already run by the time this executes.
     */
    private void validateConfiguration(
            Set<LeaseCurrency> currencies,
            LeaseCurrency settlementCurrency,
            LeaseCurrency defaultCurrency,
            Set<Integer> terms,
            Integer defaultTermMonths,
            PercentageRangeDto initialPayment,
            PercentageRangeDto buyout,
            Map<LeaseType, BigDecimal> leaseTypeRates,
            LeaseType defaultLeaseType,
            LocalDate validFrom,
            LocalDate validTo) {

        if (!currencies.contains(settlementCurrency)) {
            throw new InvalidLeaseProductConfigurationException(
                    "settlementCurrency '" + settlementCurrency + "' must be one of the accepted currencies " + currencies);
        }
        if (!currencies.contains(defaultCurrency)) {
            throw new InvalidLeaseProductConfigurationException(
                    "defaultCurrency '" + defaultCurrency + "' must be one of the accepted currencies " + currencies);
        }

        for (Integer term : terms) {
            if (term == null || term < MIN_TERM_MONTHS || term > MAX_TERM_MONTHS) {
                throw new InvalidLeaseProductConfigurationException(
                        "term " + term + " must be between " + MIN_TERM_MONTHS + " and " + MAX_TERM_MONTHS + " months");
            }
        }
        if (!terms.contains(defaultTermMonths)) {
            throw new InvalidLeaseProductConfigurationException(
                    "defaultTermMonths " + defaultTermMonths + " must be one of the configured terms " + terms);
        }

        validateRange(initialPayment, "initialPayment");
        validateRange(buyout, "buyout");

        for (Map.Entry<LeaseType, BigDecimal> entry : leaseTypeRates.entrySet()) {
            BigDecimal rate = entry.getValue();
            if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0 || rate.compareTo(MAX_ANNUAL_RATE_PERCENT) > 0) {
                throw new InvalidLeaseProductConfigurationException(
                        "annualRatePercent for leaseType '" + entry.getKey() + "' must be > 0 and <= " + MAX_ANNUAL_RATE_PERCENT);
            }
        }
        if (!leaseTypeRates.containsKey(defaultLeaseType)) {
            throw new InvalidLeaseProductConfigurationException(
                    "defaultLeaseType '" + defaultLeaseType + "' must be one of the configured leaseTypes " + leaseTypeRates.keySet());
        }

        if (validFrom != null && validTo != null && validTo.isBefore(validFrom)) {
            throw new InvalidLeaseProductConfigurationException("validTo must not be before validFrom");
        }
    }

    private void validateRange(PercentageRangeDto range, String fieldName) {
        if (range.minPercent() == null || range.minPercent().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidLeaseProductConfigurationException(fieldName + ".min must be >= 0");
        }
        if (range.maxPercent() == null || range.maxPercent().compareTo(range.minPercent()) < 0) {
            throw new InvalidLeaseProductConfigurationException(fieldName + ".max must be >= min");
        }
        if (range.defaultPercent() == null
                || range.defaultPercent().compareTo(range.minPercent()) < 0
                || range.defaultPercent().compareTo(range.maxPercent()) > 0) {
            throw new InvalidLeaseProductConfigurationException(fieldName + ".default must be within [min, max]");
        }
        if (range.stepPercent() == null || range.stepPercent().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidLeaseProductConfigurationException(fieldName + ".step must be > 0");
        }
    }
}
