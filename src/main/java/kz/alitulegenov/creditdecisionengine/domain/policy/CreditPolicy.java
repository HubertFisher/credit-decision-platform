package kz.alitulegenov.creditdecisionengine.domain.policy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record CreditPolicy(
    BigDecimal minProductAmount,
    BigDecimal maxProductAmount,
    BigDecimal amountStep,
    int minTermMonths,
    int maxTermMonths,
    List<Integer> candidateTerms,
    int minAgeAtApplication,
    int maxAgeAtMaturity,
    int maxAllowedDelinquencyDays,
    int minCreditScore,
    BigDecimal maxDtiRatio,
    BigDecimal subsistenceMinimum
) {
    public CreditPolicy {
        Objects.requireNonNull(minProductAmount, "minProductAmount must not be null");
        Objects.requireNonNull(maxProductAmount, "maxProductAmount must not be null");
        Objects.requireNonNull(amountStep, "amountStep must not be null");
        Objects.requireNonNull(candidateTerms, "candidateTerms must not be null");
        Objects.requireNonNull(maxDtiRatio, "maxDtiRatio must not be null");
        Objects.requireNonNull(subsistenceMinimum, "subsistenceMinimum must not be null");
        candidateTerms = List.copyOf(candidateTerms);
    }

    public static CreditPolicy defaultPolicy() {
        return new CreditPolicy(
            new BigDecimal("100000.00"),
            new BigDecimal("10000000.00"),
            new BigDecimal("10000.00"),
            6,
            60,
            List.of(6, 12, 18, 24, 36, 48, 60),
            21,
            65,
            30,
            520,
            new BigDecimal("0.50"),
            new BigDecimal("85000.00")
        );
    }
}
