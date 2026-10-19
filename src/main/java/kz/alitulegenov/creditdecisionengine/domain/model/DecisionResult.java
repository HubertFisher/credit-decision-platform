package kz.alitulegenov.creditdecisionengine.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import kz.alitulegenov.creditdecisionengine.domain.policy.RiskGrade;

/**
 * Complete, immutable output of the credit decision evaluation pipeline.
 *
 * <p>Contains the final verdict, risk-based pricing assessment, solvency metrics,
 * machine-readable reason codes, and generated commercial loan offers.
 *
 * @param status                 final decision outcome (APPROVED, COUNTER_OFFER, or REJECTED)
 * @param riskGrade              assigned internal risk tier (or REJECT if hard-stopped)
 * @param assignedAnnualRate     individualized nominal annual interest rate
 * @param riskExposureCap        maximum principal limit allowed for the assigned risk grade
 * @param currentDtiRatio        borrower's pre-loan debt-to-income ratio
 * @param availableMonthlyBudget maximum monthly installment affordable for the new loan
 * @param reasonCodes            ordered list of reason codes explaining the decision
 * @param offers                 generated loan offers (empty when REJECTED)
 */
public record DecisionResult(
    DecisionStatus status,
    RiskGrade riskGrade,
    BigDecimal assignedAnnualRate,
    BigDecimal riskExposureCap,
    BigDecimal currentDtiRatio,
    BigDecimal availableMonthlyBudget,
    List<DecisionReasonCode> reasonCodes,
    List<LoanOffer> offers
) {
    public DecisionResult {
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(riskGrade, "riskGrade must not be null");
        Objects.requireNonNull(assignedAnnualRate, "assignedAnnualRate must not be null");
        Objects.requireNonNull(riskExposureCap, "riskExposureCap must not be null");
        Objects.requireNonNull(currentDtiRatio, "currentDtiRatio must not be null");
        Objects.requireNonNull(availableMonthlyBudget, "availableMonthlyBudget must not be null");
        Objects.requireNonNull(reasonCodes, "reasonCodes must not be null");
        Objects.requireNonNull(offers, "offers must not be null");
        reasonCodes = List.copyOf(reasonCodes);
        offers = List.copyOf(offers);
    }
}
