package kz.alitulegenov.creditdecisionengine.domain.affordability;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import kz.alitulegenov.creditdecisionengine.domain.model.DecisionInput;
import kz.alitulegenov.creditdecisionengine.domain.model.DecisionReasonCode;
import kz.alitulegenov.creditdecisionengine.domain.policy.CreditPolicy;
import org.springframework.stereotype.Component;

/**
 * Stage 3 of the Decision Pipeline: Dual-Constraint Affordability Calculator.
 *
 * <p>Determines the borrower's maximum available monthly payment budget for a new loan by
 * enforcing two independent solvency limits simultaneously:
 * <ol>
 *   <li><b>Regulatory Debt-to-Income (DTI) ceiling:</b>
 *       {@code dtiBudget = (monthlyIncome * maxDtiRatio) - existingMonthlyPayments}</li>
 *   <li><b>Subsistence Minimum protection:</b>
 *       {@code subsistenceBudget = monthlyIncome - subsistenceMinimum - existingMonthlyPayments}</li>
 * </ol>
 *
 * <p>The effective available budget is the stricter (minimum) of the two constraints:
 * {@code availableMonthlyBudget = max(0, min(dtiBudget, subsistenceBudget))}.
 */
@Component
public class AffordabilityCalculator {

    private static final int MONEY_SCALE = 2;
    private static final int RATIO_SCALE = 4;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

    /**
     * Result of the borrower affordability assessment.
     *
     * @param currentDtiRatio        existing debt-to-income ratio prior to the new loan (4 decimal places)
     * @param availableMonthlyBudget maximum monthly installment affordable for the new loan (in KZT)
     * @param rejectionReasons       solvency rejection codes if either budget constraint is non-positive
     */
    public record AffordabilityResult(
            BigDecimal currentDtiRatio,
            BigDecimal availableMonthlyBudget,
            List<DecisionReasonCode> rejectionReasons
    ) {
        public AffordabilityResult {
            Objects.requireNonNull(currentDtiRatio, "currentDtiRatio must not be null");
            Objects.requireNonNull(availableMonthlyBudget, "availableMonthlyBudget must not be null");
            Objects.requireNonNull(rejectionReasons, "rejectionReasons must not be null");
            rejectionReasons = List.copyOf(rejectionReasons);
        }

        /**
         * Returns {@code true} if the borrower has a strictly positive monthly payment budget
         * and triggered no solvency rejections.
         */
        public boolean isAffordable() {
            return rejectionReasons.isEmpty() && availableMonthlyBudget.compareTo(BigDecimal.ZERO) > 0;
        }
    }

    /**
     * Evaluates the borrower's current DTI ratio and available monthly budget for a new loan.
     */
    public AffordabilityResult evaluate(DecisionInput input, CreditPolicy policy) {
        Objects.requireNonNull(input, "input must not be null");
        Objects.requireNonNull(policy, "policy must not be null");

        BigDecimal income = input.monthlyIncome();
        BigDecimal existingPayments = input.existingMonthlyPayments();

        // Pre-loan DTI ratio = existingMonthlyPayments / monthlyIncome
        BigDecimal currentDtiRatio = existingPayments.divide(income, RATIO_SCALE, ROUNDING_MODE);

        // Constraint 1: DTI ceiling (e.g. 50% of net monthly income minus existing obligations)
        BigDecimal maxTotalDtiPayment = income
                .multiply(policy.maxDtiRatio())
                .setScale(MONEY_SCALE, ROUNDING_MODE);
        BigDecimal dtiBudget = maxTotalDtiPayment.subtract(existingPayments);

        // Constraint 2: Disposable income above the statutory subsistence minimum
        BigDecimal subsistenceBudget = income
                .subtract(policy.subsistenceMinimum())
                .subtract(existingPayments)
                .setScale(MONEY_SCALE, ROUNDING_MODE);

        List<DecisionReasonCode> reasons = new ArrayList<>();
        if (dtiBudget.compareTo(BigDecimal.ZERO) <= 0) {
            reasons.add(DecisionReasonCode.DTI_LIMIT_EXCEEDED);
        }
        if (subsistenceBudget.compareTo(BigDecimal.ZERO) <= 0) {
            reasons.add(DecisionReasonCode.INSUFFICIENT_DISPOSABLE_INCOME);
        }

        // Select the stricter constraint and clamp negative values to 0.00
        BigDecimal rawAvailable = dtiBudget.min(subsistenceBudget);
        BigDecimal availableBudget = rawAvailable.max(BigDecimal.ZERO).setScale(MONEY_SCALE, ROUNDING_MODE);

        return new AffordabilityResult(currentDtiRatio, availableBudget, reasons);
    }
}
