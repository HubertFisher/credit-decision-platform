package kz.alitulegenov.creditdecisionengine.domain.affordability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import kz.alitulegenov.creditdecisionengine.domain.affordability.AffordabilityCalculator.AffordabilityResult;
import kz.alitulegenov.creditdecisionengine.domain.model.DecisionInput;
import kz.alitulegenov.creditdecisionengine.domain.model.DecisionReasonCode;
import kz.alitulegenov.creditdecisionengine.domain.policy.CreditPolicy;
import org.junit.jupiter.api.Test;

class AffordabilityCalculatorTest {

    private final AffordabilityCalculator calculator = new AffordabilityCalculator();
    private final CreditPolicy policy = CreditPolicy.defaultPolicy();

    @Test
    void shouldUseDtiBudgetWhenIncomeIsHigh() {
        // Income 450,000; Existing 80,000 -> DTI budget = 225,000 - 80,000 = 145,000
        // Subsistence budget = 450,000 - 85,000 - 80,000 = 285,000
        DecisionInput input = inputWithIncomeAndPayments("450000.00", "80000.00");

        AffordabilityResult result = calculator.evaluate(input, policy);

        assertTrue(result.isAffordable());
        assertEquals(new BigDecimal("0.1778"), result.currentDtiRatio());
        assertEquals(new BigDecimal("145000.00"), result.availableMonthlyBudget());
        assertTrue(result.rejectionReasons().isEmpty());
    }

    @Test
    void shouldUseSubsistenceBudgetWhenIncomeIsLow() {
        // Income 120,000; Existing 0 -> DTI budget = 60,000
        // Subsistence budget = 120,000 - 85,000 = 35,000 (stricter)
        DecisionInput input = inputWithIncomeAndPayments("120000.00", "0.00");

        AffordabilityResult result = calculator.evaluate(input, policy);

        assertTrue(result.isAffordable());
        assertEquals(new BigDecimal("0.0000"), result.currentDtiRatio());
        assertEquals(new BigDecimal("35000.00"), result.availableMonthlyBudget());
        assertTrue(result.rejectionReasons().isEmpty());
    }

    @Test
    void shouldRejectWhenDtiLimitExceeded() {
        // Income 400,000; Existing 200,000 -> DTI budget = 0
        DecisionInput input = inputWithIncomeAndPayments("400000.00", "200000.00");

        AffordabilityResult result = calculator.evaluate(input, policy);

        assertFalse(result.isAffordable());
        assertEquals(new BigDecimal("0.5000"), result.currentDtiRatio());
        assertEquals(new BigDecimal("0.00"), result.availableMonthlyBudget());
        assertEquals(List.of(DecisionReasonCode.DTI_LIMIT_EXCEEDED), result.rejectionReasons());
    }

    @Test
    void shouldRejectWhenSubsistenceMinimumNotMet() {
        // Income 120,000; Existing 40,000 -> DTI budget = 20,000 (>0), Subsistence = 120,000 - 85,000 - 40,000 = -5,000
        DecisionInput input = inputWithIncomeAndPayments("120000.00", "40000.00");

        AffordabilityResult result = calculator.evaluate(input, policy);

        assertFalse(result.isAffordable());
        assertEquals(new BigDecimal("0.3333"), result.currentDtiRatio());
        assertEquals(new BigDecimal("0.00"), result.availableMonthlyBudget());
        assertEquals(List.of(DecisionReasonCode.INSUFFICIENT_DISPOSABLE_INCOME), result.rejectionReasons());
    }

    @Test
    void shouldCollectBothReasonsWhenBothLimitsExceeded() {
        // Income 100,000; Existing 60,000 -> DTI budget = -10,000; Subsistence = -45,000
        DecisionInput input = inputWithIncomeAndPayments("100000.00", "60000.00");

        AffordabilityResult result = calculator.evaluate(input, policy);

        assertFalse(result.isAffordable());
        assertEquals(new BigDecimal("0.00"), result.availableMonthlyBudget());
        assertEquals(
                List.of(
                        DecisionReasonCode.DTI_LIMIT_EXCEEDED,
                        DecisionReasonCode.INSUFFICIENT_DISPOSABLE_INCOME
                ),
                result.rejectionReasons()
        );
    }

    private DecisionInput inputWithIncomeAndPayments(String income, String existingPayments) {
        return new DecisionInput(
                30,
                new BigDecimal(income),
                new BigDecimal(existingPayments),
                700,
                false,
                0,
                new BigDecimal("1000000.00"),
                12
        );
    }
}
