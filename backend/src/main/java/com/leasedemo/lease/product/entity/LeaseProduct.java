package com.leasedemo.lease.product.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import com.leasedemo.lease.quote.model.LeaseCurrency;
import com.leasedemo.lease.quote.model.LeaseType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A Lease Product configuration — the backend-owned authority for both:
 *
 * <ol>
 *   <li>which options Angular renders in the Lease Quote form, and</li>
 *   <li>whether a submitted {@code LeaseQuoteRequest} is valid.</li>
 * </ol>
 *
 * <p>{@code market} models a BUSINESS MARKET (e.g. {@code PL}, {@code DE}),
 * intentionally decoupled from UI language (M5.1.3 will introduce Transloco
 * for the latter, unrelated concern).
 *
 * <p>Small immutable configuration collections (currencies, terms, lease
 * types + rate) are modeled with {@link ElementCollection}s mapping onto the
 * {@code lease_product_currency} / {@code lease_product_term} /
 * {@code lease_product_lease_type} child tables — no JSON/serialized
 * collections are used.
 */
@Entity
@Table(name = "lease_product")
public class LeaseProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "market", nullable = false, length = 10)
    private String market;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "initial_payment_min_percent", nullable = false)
    private BigDecimal initialPaymentMinPercent;

    @Column(name = "initial_payment_max_percent", nullable = false)
    private BigDecimal initialPaymentMaxPercent;

    @Column(name = "initial_payment_default_percent", nullable = false)
    private BigDecimal initialPaymentDefaultPercent;

    @Column(name = "initial_payment_step_percent", nullable = false)
    private BigDecimal initialPaymentStepPercent;

    @Column(name = "buyout_min_percent", nullable = false)
    private BigDecimal buyoutMinPercent;

    @Column(name = "buyout_max_percent", nullable = false)
    private BigDecimal buyoutMaxPercent;

    @Column(name = "buyout_default_percent", nullable = false)
    private BigDecimal buyoutDefaultPercent;

    @Column(name = "buyout_step_percent", nullable = false)
    private BigDecimal buyoutStepPercent;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_currency", nullable = false, length = 3)
    private LeaseCurrency defaultCurrency;

    /**
     * The currency in which this product's lease amounts are calculated
     * and settled (M5.1.3.2) — distinct from {@link #currencies}, which
     * are the accepted VEHICLE PRICE currencies. Must be present in
     * {@link #currencies}. The sole source of truth: never derived from
     * {@link #market} at runtime.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_currency", nullable = false, length = 3)
    private LeaseCurrency settlementCurrency;

    @Column(name = "default_term_months", nullable = false)
    private Integer defaultTermMonths;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_lease_type", nullable = false, length = 20)
    private LeaseType defaultLeaseType;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "lease_product_currency", joinColumns = @JoinColumn(name = "product_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "currency")
    private Set<LeaseCurrency> currencies = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "lease_product_term", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "term_months")
    private Set<Integer> termsMonths = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "lease_product_lease_type", joinColumns = @JoinColumn(name = "product_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "lease_type")
    @Column(name = "annual_rate_percent")
    private Map<LeaseType, BigDecimal> leaseTypeAnnualRatePercent = new LinkedHashMap<>();

    protected LeaseProduct() {
        // required by JPA
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getMarket() {
        return market;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public BigDecimal getInitialPaymentMinPercent() {
        return initialPaymentMinPercent;
    }

    public BigDecimal getInitialPaymentMaxPercent() {
        return initialPaymentMaxPercent;
    }

    public BigDecimal getInitialPaymentDefaultPercent() {
        return initialPaymentDefaultPercent;
    }

    public BigDecimal getInitialPaymentStepPercent() {
        return initialPaymentStepPercent;
    }

    public BigDecimal getBuyoutMinPercent() {
        return buyoutMinPercent;
    }

    public BigDecimal getBuyoutMaxPercent() {
        return buyoutMaxPercent;
    }

    public BigDecimal getBuyoutDefaultPercent() {
        return buyoutDefaultPercent;
    }

    public BigDecimal getBuyoutStepPercent() {
        return buyoutStepPercent;
    }

    public LeaseCurrency getDefaultCurrency() {
        return defaultCurrency;
    }

    public LeaseCurrency getSettlementCurrency() {
        return settlementCurrency;
    }

    public Integer getDefaultTermMonths() {
        return defaultTermMonths;
    }

    public LeaseType getDefaultLeaseType() {
        return defaultLeaseType;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public Set<LeaseCurrency> getCurrencies() {
        return currencies;
    }

    public Set<Integer> getTermsMonths() {
        return termsMonths;
    }

    public Map<LeaseType, BigDecimal> getLeaseTypeAnnualRatePercent() {
        return leaseTypeAnnualRatePercent;
    }

    /**
     * Whether this product is usable "now" — enabled AND (if set)
     * {@code validFrom <= today <= validTo}.
     */
    public boolean isValidOn(LocalDate today) {
        if (!enabled) {
            return false;
        }
        if (validFrom != null && today.isBefore(validFrom)) {
            return false;
        }
        return validTo == null || !today.isAfter(validTo);
    }
}

