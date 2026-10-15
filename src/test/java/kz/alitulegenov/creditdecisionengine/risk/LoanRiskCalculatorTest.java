package kz.alitulegenov.creditdecisionengine.risk;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class LoanRiskCalculatorTest {

    private final LoanRiskCalculator loanRiskCalculator = new LoanRiskCalculator();

    @Test
    void shouldReturnZeroRiskWhenNoRiskFactorsApply() {
        ScoringInput input = new ScoringInput(
                new BigDecimal("1000000"),
                new BigDecimal("3000000"),
                24
        );

        ScoringResult result = loanRiskCalculator.calculateRisk(input);

        assertResult(result, 0, 0, 0, 0);
    }

    @Test
    void shouldAddTwentyPointsWhenTermExceedsThirtySixMonths() {
        ScoringInput input = new ScoringInput(
                new BigDecimal("1000000"),
                new BigDecimal("3000000"),
                37
        );

        ScoringResult result = loanRiskCalculator.calculateRisk(input);

        assertResult(result, 20, 0, 20, 0);
    }

    @Test
    void shouldNotAddTermRiskWhenTermIsExactlyThirtySixMonths() {
        ScoringInput input = new ScoringInput(
                new BigDecimal("350000"),
                new BigDecimal("3000000"),
                36
        );

        ScoringResult result = loanRiskCalculator.calculateRisk(input);

        assertResult(result, 0, 0, 0, 0);
    }

    @Test
    void shouldAddThirtyPointsWhenIncomeIsBelowThreshold() {
        ScoringInput input = new ScoringInput(
                new BigDecimal("250000"),
                new BigDecimal("2500000"),
                36
        );

        ScoringResult result = loanRiskCalculator.calculateRisk(input);

        assertResult(result, 30, 0, 0, 30);
    }

    @Test
    void shouldNotAddIncomeRiskWhenIncomeIsExactlyThreshold() {
        ScoringInput input = new ScoringInput(
                new BigDecimal("300000"),
                new BigDecimal("3000000"),
                36
        );

        ScoringResult result = loanRiskCalculator.calculateRisk(input);

        assertResult(result, 0, 0, 0, 0);
    }

    @Test
    void shouldAddFortyPointsWhenRequestedAmountExceedsTenTimesIncome() {
        ScoringInput input = new ScoringInput(
                new BigDecimal("300000"),
                new BigDecimal("3000003"),
                36
        );

        ScoringResult result = loanRiskCalculator.calculateRisk(input);

        assertResult(result, 40, 40, 0, 0);
    }

    @Test
    void shouldSumAllRiskFactors() {
        ScoringInput input = new ScoringInput(
                new BigDecimal("250000"),
                new BigDecimal("2500000.01"),
                37
        );

        ScoringResult result = loanRiskCalculator.calculateRisk(input);

        assertResult(result, 90, 40, 20, 30);
    }

    @Test
    void shouldNotAddAmountRiskWhenRequestedAmountIsExactlyTenTimesIncome() {
        ScoringInput input = new ScoringInput(
                new BigDecimal("300000"),
                new BigDecimal("3000000"),
                24
        );

        ScoringResult result = loanRiskCalculator.calculateRisk(input);

        assertResult(result, 0, 0, 0, 0);
    }

    @Test
    void shouldKeepBreakdownImmutableWhenOriginalListChanges() {
        ScoringRuleResult lowIncome = new ScoringRuleResult(
                ScoringRuleCode.LOW_INCOME,
                true,
                30
        );
        List<ScoringRuleResult> rules = new ArrayList<>(List.of(lowIncome));

        ScoringResult result = new ScoringResult("legacy-v1", 30, rules);
        rules.clear();

        Assertions.assertEquals(List.of(lowIncome), result.breakdown());
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> result.breakdown().clear()
        );
    }

    private void assertResult(
            ScoringResult result,
            int expectedScore,
            int amountPoints,
            int termPoints,
            int incomePoints
    ) {
        Assertions.assertEquals("legacy-v1", result.policyVersion());
        Assertions.assertEquals(expectedScore, result.riskScore());
        Assertions.assertEquals(
                List.of(
                        new ScoringRuleResult(
                                ScoringRuleCode.AMOUNT_INCOME_RATIO_HIGH,
                                amountPoints > 0,
                                amountPoints
                        ),
                        new ScoringRuleResult(
                                ScoringRuleCode.LONG_TERM,
                                termPoints > 0,
                                termPoints
                        ),
                        new ScoringRuleResult(
                                ScoringRuleCode.LOW_INCOME,
                                incomePoints > 0,
                                incomePoints
                        )
                ),
                result.breakdown()
        );
    }
}
