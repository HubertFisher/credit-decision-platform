package kz.alitulegenov.creditdecisionengine.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import java.time.Instant;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
public class LoanDecision {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal income;

    @Column(nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal requestedAmount;

    @Column(nullable = false, updatable = false)
    private Integer termMonths;

    @Column(nullable = false, updatable = false)
    private int riskScore;

    @Column(nullable = false, updatable = false)
    private boolean approved;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;


    protected LoanDecision() {}
    public LoanDecision(BigDecimal income,
                        BigDecimal requestedAmount,
                        Integer termMonths,
                        int riskScore,
                        boolean approved ) {
        this.income = income;
        this.requestedAmount = requestedAmount;
        this.termMonths = termMonths;
        this.riskScore = riskScore;
        this.approved = approved;
        this.createdAt = Instant.now();
    }
    public UUID getId() {return id;}
    public BigDecimal getIncome() {return income;}
    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }
    public Integer getTermMonths() {
        return termMonths;
    }
    public int getRiskScore() {
        return riskScore;
    }
    public boolean isApproved() {
        return approved;
    }
    public Instant getCreatedAt() {return createdAt;}

}
