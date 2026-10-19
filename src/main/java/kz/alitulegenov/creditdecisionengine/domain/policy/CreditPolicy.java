package kz.alitulegenov.creditdecisionengine.domain.policy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Immutable configuration of credit risk parameters, product limits, and regulatory solvency thresholds.
 *
 * @param minProductAmount          minimum loan principal issued by the bank (in KZT)
 * @param maxProductAmount          maximum loan principal issued for an unsecured consumer loan (in KZT)
 * @param amountStep                rounding step for counter-offer principal amounts (e.g. 10,000 KZT)
 * @param minTermMonths             shortest supported loan duration in months
 * @param maxTermMonths             longest supported loan duration in months
 * @param candidateTerms            ordered grid of standard loan durations used for counter-offer generation
 * @param minAgeAtApplication       minimum eligible applicant age in years at application time
 * @param maxAgeAtMaturity          maximum eligible applicant age in years at loan maturity
 * @param maxAllowedDelinquencyDays maximum tolerated historical delinquency in days over the past 12 months
 * @param minCreditScore            minimum external credit bureau score required to pass hard-stop screening
 * @param maxDtiRatio               maximum allowed Debt-to-Income (DTI) ratio as a decimal fraction (e.g. 0.50)
 * @param subsistenceMinimum        statutory minimum monthly living allowance that must remain after all debt payments
 */
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

    /**
     * Returns the standard commercial consumer lending policy.
     */
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
