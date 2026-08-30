package com.leasedemo.lease.application.entity;

import com.leasedemo.entity.Customer;
import com.leasedemo.lease.application.dto.InsuranceCoverageSnapshot;
import com.leasedemo.lease.application.model.ApplicationStatus;
import com.leasedemo.lease.quote.model.LeaseCurrency;
import com.leasedemo.lease.quote.model.LeaseType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * A durable, submitted Lease Application (M5.3).
 *
 * <p><strong>Snapshot principle</strong> (M5.3 §7): every business value
 * below is a SNAPSHOT captured at submission time. If an ADMIN later
 * changes the underlying {@code LeaseProduct}, this row must NOT change —
 * it continues to represent exactly what was agreed/calculated when the
 * customer applied. Nothing here is dynamically reconstructed from the
 * current {@code LeaseProduct} state.
 *
 * <p>This entity is the durable event SOURCE for M5.5 Kafka — no
 * publishing/outbox logic is added in this milestone.
 */
@Entity
@Table(name = "lease_application")
public class LeaseApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "product_code", nullable = false)
    private String productCode;

    @Column(name = "product_name_snapshot", nullable = false)
    private String productNameSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ApplicationStatus status;

    @Column(name = "credit_score", nullable = false)
    private Integer creditScore;

    @Column(name = "monthly_net_income", nullable = false, precision = 14, scale = 2)
    private BigDecimal monthlyNetIncome;

    @Column(name = "monthly_obligations", nullable = false, precision = 14, scale = 2)
    private BigDecimal monthlyObligations;

    @Column(name = "vehicle_price_original", nullable = false, precision = 14, scale = 2)
    private BigDecimal vehiclePriceOriginal;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_price_currency", nullable = false, length = 3)
    private LeaseCurrency vehiclePriceCurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_currency", nullable = false, length = 3)
    private LeaseCurrency settlementCurrency;

    @Column(name = "exchange_rate", nullable = false, precision = 14, scale = 6)
    private BigDecimal exchangeRate;

    @Column(name = "exchange_rate_date")
    private LocalDate exchangeRateDate;

    @Column(name = "vehicle_price_settlement", nullable = false, precision = 14, scale = 2)
    private BigDecimal vehiclePriceSettlement;

    @Column(name = "term_months", nullable = false)
    private Integer termMonths;

    @Column(name = "initial_payment_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal initialPaymentPercent;

    @Column(name = "initial_payment", nullable = false, precision = 14, scale = 2)
    private BigDecimal initialPayment;

    @Column(name = "buyout_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal buyoutPercent;

    @Column(name = "buyout", nullable = false, precision = 14, scale = 2)
    private BigDecimal buyout;

    @Enumerated(EnumType.STRING)
    @Column(name = "lease_type", nullable = false, length = 20)
    private LeaseType leaseType;

    @Column(name = "annual_rate_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal annualRatePercent;

    @Column(name = "financed_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal financedAmount;

    @Column(name = "monthly_payment", nullable = false, precision = 14, scale = 2)
    private BigDecimal monthlyPayment;

    @Column(name = "total_lease_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalLeaseCost;

    @Column(name = "estimated_vat", nullable = false, precision = 14, scale = 2)
    private BigDecimal estimatedVat;

    @Column(name = "insurance_monthly_premium", nullable = false, precision = 14, scale = 2)
    private BigDecimal insuranceMonthlyPremium;

    /**
     * Validated, backend-priced insurance snapshot (M5.3 §11) persisted as
     * native PostgreSQL JSONB via Hibernate 6's {@code SqlTypes.JSON}
     * mapping — no manual JSON string concatenation.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "insurance_configuration", nullable = false, columnDefinition = "jsonb")
    private List<InsuranceCoverageSnapshot> insuranceConfiguration;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    protected LeaseApplication() {
        // required by JPA
    }

    public LeaseApplication(
            Customer customer,
            String productCode,
            String productNameSnapshot,
            ApplicationStatus status,
            Integer creditScore,
            BigDecimal monthlyNetIncome,
            BigDecimal monthlyObligations,
            BigDecimal vehiclePriceOriginal,
            LeaseCurrency vehiclePriceCurrency,
            LeaseCurrency settlementCurrency,
            BigDecimal exchangeRate,
            LocalDate exchangeRateDate,
            BigDecimal vehiclePriceSettlement,
            Integer termMonths,
            BigDecimal initialPaymentPercent,
            BigDecimal initialPayment,
            BigDecimal buyoutPercent,
            BigDecimal buyout,
            LeaseType leaseType,
            BigDecimal annualRatePercent,
            BigDecimal financedAmount,
            BigDecimal monthlyPayment,
            BigDecimal totalLeaseCost,
            BigDecimal estimatedVat,
            BigDecimal insuranceMonthlyPremium,
            List<InsuranceCoverageSnapshot> insuranceConfiguration) {
        this.customer = customer;
        this.productCode = productCode;
        this.productNameSnapshot = productNameSnapshot;
        this.status = status;
        this.creditScore = creditScore;
        this.monthlyNetIncome = monthlyNetIncome;
        this.monthlyObligations = monthlyObligations;
        this.vehiclePriceOriginal = vehiclePriceOriginal;
        this.vehiclePriceCurrency = vehiclePriceCurrency;
        this.settlementCurrency = settlementCurrency;
        this.exchangeRate = exchangeRate;
        this.exchangeRateDate = exchangeRateDate;
        this.vehiclePriceSettlement = vehiclePriceSettlement;
        this.termMonths = termMonths;
        this.initialPaymentPercent = initialPaymentPercent;
        this.initialPayment = initialPayment;
        this.buyoutPercent = buyoutPercent;
        this.buyout = buyout;
        this.leaseType = leaseType;
        this.annualRatePercent = annualRatePercent;
        this.financedAmount = financedAmount;
        this.monthlyPayment = monthlyPayment;
        this.totalLeaseCost = totalLeaseCost;
        this.estimatedVat = estimatedVat;
        this.insuranceMonthlyPremium = insuranceMonthlyPremium;
        this.insuranceConfiguration = insuranceConfiguration;
    }

    /**
     * Sets {@code submittedAt}/{@code decidedAt} on insert — the backend is
     * the sole owner of these timestamps, never the client. Since M5.3 has
     * no manual-review workflow, the decision is always made synchronously
     * at submission time.
     */
    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.submittedAt = now;
        this.decidedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public String getProductCode() {
        return productCode;
    }

    public String getProductNameSnapshot() {
        return productNameSnapshot;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public Integer getCreditScore() {
        return creditScore;
    }

    public BigDecimal getMonthlyNetIncome() {
        return monthlyNetIncome;
    }

    public BigDecimal getMonthlyObligations() {
        return monthlyObligations;
    }

    public BigDecimal getVehiclePriceOriginal() {
        return vehiclePriceOriginal;
    }

    public LeaseCurrency getVehiclePriceCurrency() {
        return vehiclePriceCurrency;
    }

    public LeaseCurrency getSettlementCurrency() {
        return settlementCurrency;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public LocalDate getExchangeRateDate() {
        return exchangeRateDate;
    }

    public BigDecimal getVehiclePriceSettlement() {
        return vehiclePriceSettlement;
    }

    public Integer getTermMonths() {
        return termMonths;
    }

    public BigDecimal getInitialPaymentPercent() {
        return initialPaymentPercent;
    }

    public BigDecimal getInitialPayment() {
        return initialPayment;
    }

    public BigDecimal getBuyoutPercent() {
        return buyoutPercent;
    }

    public BigDecimal getBuyout() {
        return buyout;
    }

    public LeaseType getLeaseType() {
        return leaseType;
    }

    public BigDecimal getAnnualRatePercent() {
        return annualRatePercent;
    }

    public BigDecimal getFinancedAmount() {
        return financedAmount;
    }

    public BigDecimal getMonthlyPayment() {
        return monthlyPayment;
    }

    public BigDecimal getTotalLeaseCost() {
        return totalLeaseCost;
    }

    public BigDecimal getEstimatedVat() {
        return estimatedVat;
    }

    public BigDecimal getInsuranceMonthlyPremium() {
        return insuranceMonthlyPremium;
    }

    public List<InsuranceCoverageSnapshot> getInsuranceConfiguration() {
        return insuranceConfiguration;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }
}
