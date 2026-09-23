package kz.alitulegenov.creditdecisionengine.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
public class LoanDecision {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private BigDecimal income;
    private BigDecimal requestedAmount;
    private Integer termMonths;

    private int riskScore;
    private boolean approved;

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
    }
    public UUID getId() {
        return id;
    }
    public BigDecimal getIncome() {
        return income;
    }

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

}
