package kz.alitulegenov.creditdecisionengine.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Normalized borrower and loan request profile supplied by the upstream Loan Origination System (LOS).
 *
 * @param applicantAgeYears             full age of the applicant in years at the time of application
 * @param monthlyIncome                 verified net monthly income after taxes (in KZT)
 * @param existingMonthlyPayments       total monthly installments across all active loans from the credit bureau (in KZT)
 * @param creditScore                   external credit bureau score (300–850 scale)
 * @param hasActiveDelinquency          whether the applicant currently has any overdue debt
 * @param maxDelinquencyDaysLast12Months worst historical delinquency duration in days over the past 12 months
 * @param requestedAmount               loan principal requested by the applicant (in KZT)
 * @param requestedTermMonths           loan duration in months requested by the applicant
 */
public record DecisionInput(
        int applicantAgeYears,
        BigDecimal monthlyIncome,
        BigDecimal existingMonthlyPayments,
        int creditScore,
        boolean hasActiveDelinquency,
        int maxDelinquencyDaysLast12Months,
        BigDecimal requestedAmount,
        int requestedTermMonths
) {
    public DecisionInput {
        Objects.requireNonNull(monthlyIncome, "monthlyIncome must not be null");
        Objects.requireNonNull(existingMonthlyPayments, "existingMonthlyPayments must not be null");
        Objects.requireNonNull(requestedAmount, "requestedAmount must not be null");
    }
}
