package kz.alitulegenov.creditdecisionengine.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import kz.alitulegenov.creditdecisionengine.domain.model.DecisionReasonCode;
import kz.alitulegenov.creditdecisionengine.domain.model.DecisionStatus;
import kz.alitulegenov.creditdecisionengine.domain.model.LoanOffer;
import kz.alitulegenov.creditdecisionengine.domain.policy.RiskGrade;

/**
 * Outbound HTTP response payload representing a processed credit decision,
 * including risk-based pricing, solvency metrics, reason codes, and generated loan offers.
 *
 * @param id                        internal unique identifier of the decision record
 * @param requestId                 idempotency request identifier supplied by the caller
 * @param status                    final decision outcome (APPROVED, COUNTER_OFFER, or REJECTED)
 * @param applicantAgeYears         applicant's age in years at application time
 * @param monthlyIncome             verified net monthly income in KZT
 * @param existingMonthlyPayments   existing monthly debt obligations in KZT
 * @param creditScore               external credit bureau score
 * @param hasActiveDelinquency      whether active delinquency was reported
 * @param maxDelinquencyDaysLast12Months maximum historical delinquency days in the past 12 months
 * @param requestedAmount           originally requested loan principal in KZT
 * @param requestedTermMonths       originally requested loan duration in months
 * @param riskGrade                 assigned internal risk grade
 * @param assignedAnnualRate        assigned nominal annual interest rate
 * @param riskExposureCap           maximum principal exposure cap for the assigned risk grade
 * @param currentDtiRatio           pre-loan debt-to-income ratio
 * @param availableMonthlyBudget    maximum affordable monthly installment for a new loan
 * @param reasonCodes               ordered list of machine-readable decision reason codes
 * @param offers                    generated loan offers (empty if REJECTED)
 * @param createdAt                 UTC timestamp when the decision was recorded
 */
public record CreditDecisionResponse(
    UUID id,
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
    List<LoanOffer> offers,
    Instant createdAt
) {
    public CreditDecisionResponse {
        Objects.requireNonNull(reasonCodes, "reasonCodes must not be null");
        Objects.requireNonNull(offers, "offers must not be null");
        reasonCodes = List.copyOf(reasonCodes);
        offers = List.copyOf(offers);
    }
}
