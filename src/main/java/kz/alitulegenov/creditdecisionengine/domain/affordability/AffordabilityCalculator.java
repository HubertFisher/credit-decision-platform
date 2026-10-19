package kz.alitulegenov.creditdecisionengine.domain.affordability;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import kz.alitulegenov.creditdecisionengine.domain.model.DecisionInput;
import kz.alitulegenov.creditdecisionengine.domain.model.DecisionReasonCode;
import kz.alitulegenov.creditdecisionengine.domain.policy.CreditPolicy;
import org.springframework.stereotype.Component;

@Component
public class AffordabilityCalculator {

    public record AffordabilityResult(
        BigDecimal currentDtiRatio,
        BigDecimal availableMonthlyBudget,
        List<DecisionReasonCode> rejectionReasons
    ) {
        public AffordabilityResult {
            rejectionReasons = List.copyOf(rejectionReasons);
        }

        public boolean isAffordable() {
            return rejectionReasons.isEmpty() && availableMonthlyBudget.compareTo(BigDecimal.ZERO) > 0;
        }
    }

    public AffordabilityResult evaluate(DecisionInput input, CreditPolicy policy) {
        BigDecimal income = input.monthlyIncome();
        BigDecimal existingPayments = input.existingMonthlyPayments();

        BigDecimal currentDtiRatio = existingPayments.divide(income, 4, RoundingMode.HALF_EVEN);

        BigDecimal maxTotalDtiPayment = income
            .multiply(policy.maxDtiRatio())
            .setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal dtiBudget = maxTotalDtiPayment.subtract(existingPayments);

        BigDecimal subsistenceBudget = income
            .subtract(policy.subsistenceMinimum())
            .subtract(existingPayments)
            .setScale(2, RoundingMode.HALF_EVEN);

        List<DecisionReasonCode> reasons = new ArrayList<>();
        if (dtiBudget.compareTo(BigDecimal.ZERO) <= 0) {
            reasons.add(DecisionReasonCode.DTI_LIMIT_EXCEEDED);
        }
        if (subsistenceBudget.compareTo(BigDecimal.ZERO) <= 0) {
            reasons.add(DecisionReasonCode.INSUFFICIENT_DISPOSABLE_INCOME);
        }

        BigDecimal rawAvailable = dtiBudget.min(subsistenceBudget);
        BigDecimal availableBudget = rawAvailable.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_EVEN);

        return new AffordabilityResult(currentDtiRatio, availableBudget, reasons);
    }
}
