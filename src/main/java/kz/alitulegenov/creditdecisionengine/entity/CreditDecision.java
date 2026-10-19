package kz.alitulegenov.creditdecisionengine.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import kz.alitulegenov.creditdecisionengine.domain.model.DecisionReasonCode;
import kz.alitulegenov.creditdecisionengine.domain.model.DecisionStatus;
import kz.alitulegenov.creditdecisionengine.domain.model.LoanOffer;
import kz.alitulegenov.creditdecisionengine.domain.policy.RiskGrade;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Immutable persistence entity representing a recorded credit decision and its full audit snapshot.
 */
@Entity
@Table(name = "credit_decision")
public class CreditDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "request_id", nullable = false, unique = true, updatable = false)
    private UUID requestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32, updatable = false)
    private DecisionStatus status;

    @Column(name = "applicant_age_years", nullable = false, updatable = false)
    private int applicantAgeYears;

    @Column(name = "monthly_income", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal monthlyIncome;

    @Column(name = "existing_monthly_payments", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal existingMonthlyPayments;

    @Column(name = "credit_score", nullable = false, updatable = false)
    private int creditScore;

    @Column(name = "has_active_delinquency", nullable = false, updatable = false)
    private boolean hasActiveDelinquency;

    @Column(name = "max_delinquency_days_12m", nullable = false, updatable = false)
    private int maxDelinquencyDaysLast12Months;

    @Column(name = "requested_amount", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal requestedAmount;

    @Column(name = "requested_term_months", nullable = false, updatable = false)
    private int requestedTermMonths;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_grade", nullable = false, length = 16, updatable = false)
    private RiskGrade riskGrade;

    @Column(name = "assigned_annual_rate", nullable = false, precision = 7, scale = 4, updatable = false)
    private BigDecimal assignedAnnualRate;

    @Column(name = "risk_exposure_cap", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal riskExposureCap;

    @Column(name = "current_dti_ratio", nullable = false, precision = 7, scale = 4, updatable = false)
    private BigDecimal currentDtiRatio;

    @Column(name = "available_monthly_budget", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal availableMonthlyBudget;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reason_codes", columnDefinition = "jsonb", nullable = false, updatable = false)
    private List<DecisionReasonCode> reasonCodes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "offers", columnDefinition = "jsonb", nullable = false, updatable = false)
    private List<LoanOffer> offers;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CreditDecision() {
    }

    public CreditDecision(
        UUID requestId,
        DecisionStatus status,
        int applicantAgeYears,
        BigDecimal monthlyIncome,
        BigDecimal existingMonthlyPayments,
        int creditScore,
        boolean hasActiveDelinquency,
        int maxDelinquencyDaysLast12Months,
        BigDecimal requestedAmount,
        int requestedTermMonths,
        RiskGrade riskGrade,
        BigDecimal assignedAnnualRate,
        BigDecimal riskExposureCap,
        BigDecimal currentDtiRatio,
        BigDecimal availableMonthlyBudget,
        List<DecisionReasonCode> reasonCodes,
        List<LoanOffer> offers
    ) {
        this.requestId = requestId;
        this.status = status;
        this.applicantAgeYears = applicantAgeYears;
        this.monthlyIncome = monthlyIncome;
        this.existingMonthlyPayments = existingMonthlyPayments;
        this.creditScore = creditScore;
        this.hasActiveDelinquency = hasActiveDelinquency;
        this.maxDelinquencyDaysLast12Months = maxDelinquencyDaysLast12Months;
        this.requestedAmount = requestedAmount;
        this.requestedTermMonths = requestedTermMonths;
        this.riskGrade = riskGrade;
        this.assignedAnnualRate = assignedAnnualRate;
        this.riskExposureCap = riskExposureCap;
        this.currentDtiRatio = currentDtiRatio;
        this.availableMonthlyBudget = availableMonthlyBudget;
        this.reasonCodes = List.copyOf(reasonCodes);
        this.offers = List.copyOf(offers);
    }

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public DecisionStatus getStatus() {
        return status;
    }

    public int getApplicantAgeYears() {
        return applicantAgeYears;
    }

    public BigDecimal getMonthlyIncome() {
        return monthlyIncome;
    }

    public BigDecimal getExistingMonthlyPayments() {
        return existingMonthlyPayments;
    }

    public int getCreditScore() {
        return creditScore;
    }

    public boolean isHasActiveDelinquency() {
        return hasActiveDelinquency;
    }

    public int getMaxDelinquencyDaysLast12Months() {
        return maxDelinquencyDaysLast12Months;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public int getRequestedTermMonths() {
        return requestedTermMonths;
    }

    public RiskGrade getRiskGrade() {
        return riskGrade;
    }

    public BigDecimal getAssignedAnnualRate() {
        return assignedAnnualRate;
    }

    public BigDecimal getRiskExposureCap() {
        return riskExposureCap;
    }

    public BigDecimal getCurrentDtiRatio() {
        return currentDtiRatio;
    }

    public BigDecimal getAvailableMonthlyBudget() {
        return availableMonthlyBudget;
    }

    public List<DecisionReasonCode> getReasonCodes() {
        return reasonCodes;
    }

    public List<LoanOffer> getOffers() {
        return offers;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
