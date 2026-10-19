package kz.alitulegenov.creditdecisionengine.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

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
