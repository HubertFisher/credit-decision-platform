package kz.alitulegenov.creditdecisionengine.risk;

import kz.alitulegenov.creditdecisionengine.dto.LoanDecisionRequest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

public class LoanRiskCalculatorTest {

    private final LoanRiskCalculator loanRiskCalculator = new LoanRiskCalculator();

    @Test
    void shouldReturnZeroRiskWhenNoRiskFactorsApply() {
        LoanDecisionRequest loanDecisionRequest = new LoanDecisionRequest(
                new BigDecimal("1000000"),
                new BigDecimal("3000000"),
                24
        );

        int risk = loanRiskCalculator.calculateRisk(loanDecisionRequest);

        Assertions.assertEquals(0, risk);
    }

    @Test
    void shouldAddTwentyPointsWhenTermExceedsThirtySixMonths() {
        LoanDecisionRequest loanDecisionRequest = new LoanDecisionRequest(
                new BigDecimal("1000000"),
                new BigDecimal("3000000"),
                37
        );

        int risk = loanRiskCalculator.calculateRisk(loanDecisionRequest);

        Assertions.assertEquals(20, risk);
    }

    @Test
    void shouldNotAddTermRiskWhenTermIsExactlyThirtySixMonths() {
        LoanDecisionRequest loanDecisionRequest = new LoanDecisionRequest(
                new BigDecimal("350000"),
                new BigDecimal("3000000"),
                36
        );

        int risk = loanRiskCalculator.calculateRisk(loanDecisionRequest);

        Assertions.assertEquals(0, risk);
    }

    @Test
    void shouldAddThirtyPointsWhenIncomeIsBelowThreshold() {
        LoanDecisionRequest loanDecisionRequest = new LoanDecisionRequest(
                new BigDecimal("250000"),
                new BigDecimal("2500000"),
                36
        );

        int risk = loanRiskCalculator.calculateRisk(loanDecisionRequest);

        Assertions.assertEquals(30, risk);
    }

    @Test
    void shouldNotAddIncomeRiskWhenIncomeIsExactlyThreshold() {
        LoanDecisionRequest loanDecisionRequest = new LoanDecisionRequest(
                new BigDecimal("300000"),
                new BigDecimal("3000000"),
                36
        );

        int risk = loanRiskCalculator.calculateRisk(loanDecisionRequest);

        Assertions.assertEquals(0, risk);
    }

    @Test
    void shouldAddFortyPointsWhenRequestedAmountExceedsTenTimesIncome() {
        LoanDecisionRequest loanDecisionRequest = new LoanDecisionRequest(
                new BigDecimal("300000"),
                new BigDecimal("3000003"),
                36
        );

        int risk = loanRiskCalculator.calculateRisk(loanDecisionRequest);

        Assertions.assertEquals(40, risk);
    }

    @Test
    void shouldSumAllRiskFactors() {
        LoanDecisionRequest loanDecisionRequest = new LoanDecisionRequest(
                new BigDecimal("250000"),
                new BigDecimal("2500000.01"),
                37
        );

        int risk = loanRiskCalculator.calculateRisk(loanDecisionRequest);

        Assertions.assertEquals(90, risk);
    }

    @Test
    void shouldNotAddAmountRiskWhenRequestedAmountIsExactlyTenTimesIncome() {
        LoanDecisionRequest loanDecisionRequest = new LoanDecisionRequest(
                new BigDecimal("300000"),
                new BigDecimal("3000000"),
                24
        );

        int risk = loanRiskCalculator.calculateRisk(loanDecisionRequest);

        Assertions.assertEquals(0, risk);
    }
}
