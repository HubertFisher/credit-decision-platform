package kz.alitulegenov.creditdecisionengine.risk;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class LoanRiskCalculator {

    public ScoringResult calculateRisk(ScoringInput input) {
        boolean lowIncome = input.monthlyIncome().compareTo(new BigDecimal(300000)) < 0;

        ScoringRuleResult incomeRule = new ScoringRuleResult(
                ScoringRuleCode.LOW_INCOME,
                lowIncome,
                lowIncome ? 30 : 0
        );

        boolean longTerm = input.termMonths() > 36;

        ScoringRuleResult termRule = new ScoringRuleResult(
                ScoringRuleCode.LONG_TERM,
                longTerm,
                longTerm ? 20 : 0
        );

        boolean amountIncomeRatioHigh = input.requestedAmount()
                .compareTo(input.monthlyIncome().multiply(BigDecimal.TEN)) > 0;

        ScoringRuleResult amountRule = new ScoringRuleResult(
                ScoringRuleCode.AMOUNT_INCOME_RATIO_HIGH,
                amountIncomeRatioHigh,
                amountIncomeRatioHigh ? 40 : 0
        );

        return new ScoringResult(
                "legacy-v1",
                incomeRule.points() + termRule.points() + amountRule.points(),
                List.of(amountRule, termRule, incomeRule)
        );
    }
}
