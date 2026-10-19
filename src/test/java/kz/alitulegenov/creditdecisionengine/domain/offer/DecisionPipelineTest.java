package kz.alitulegenov.creditdecisionengine.domain.offer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import kz.alitulegenov.creditdecisionengine.domain.affordability.AffordabilityCalculator;
import kz.alitulegenov.creditdecisionengine.domain.calculator.AnnuityLoanCalculator;
import kz.alitulegenov.creditdecisionengine.domain.model.DecisionInput;
import kz.alitulegenov.creditdecisionengine.domain.model.DecisionReasonCode;
import kz.alitulegenov.creditdecisionengine.domain.model.DecisionResult;
import kz.alitulegenov.creditdecisionengine.domain.model.DecisionStatus;
import kz.alitulegenov.creditdecisionengine.domain.model.LoanOffer;
import kz.alitulegenov.creditdecisionengine.domain.model.OfferType;
import kz.alitulegenov.creditdecisionengine.domain.policy.RiskGrade;
import kz.alitulegenov.creditdecisionengine.domain.pricing.RiskGradeResolver;
import kz.alitulegenov.creditdecisionengine.domain.rule.HardStopEvaluator;
import org.junit.jupiter.api.Test;

class DecisionPipelineTest {

    private final DecisionPipeline pipeline = new DecisionPipeline(
            new HardStopEvaluator(),
            new RiskGradeResolver(),
            new AffordabilityCalculator(),
            new AnnuityLoanCalculator()
    );

    @Test
    void shouldApproveExactRequestedTermsWhenAllCriteriaAndBudgetPass() {
        DecisionInput input = new DecisionInput(
                32,
                new BigDecimal("500000.00"),
                new BigDecimal("50000.00"),
                780,
                false,
                0,
                new BigDecimal("2000000.00"),
                24
        );

        DecisionResult result = pipeline.evaluate(input);

        assertEquals(DecisionStatus.APPROVED, result.status());
        assertEquals(RiskGrade.GRADE_A, result.riskGrade());
        assertEquals(new BigDecimal("0.1600"), result.assignedAnnualRate());
        assertEquals(List.of(DecisionReasonCode.APPROVED_AS_REQUESTED), result.reasonCodes());
        assertEquals(1, result.offers().size());

        LoanOffer offer = result.offers().getFirst();
        assertEquals(OfferType.REQUESTED_TERMS, offer.offerType());
        assertEquals(new BigDecimal("2000000.00"), offer.principalAmount());
        assertEquals(24, offer.termMonths());
    }

    @Test
    void shouldGenerateBothReducedAmountAndExtendedTermCounterOffersWhenInstallmentExceedsBudget() {
        // Income 450,000; Existing 80,000 -> Available budget = 145,000 KZT
        // Score 710 -> GRADE_B (20% APR, 6,000,000 cap)
        // Requested: 3,000,000 on 12 months (PMT = 277,903.52 > 145,000)
        // Option A (12 months): max principal = 1,560,000 KZT
        // Option B (extended term for 3,000,000): 36 months gives PMT = 111,490.75 <= 145,000
        DecisionInput input = new DecisionInput(
                32,
                new BigDecimal("450000.00"),
                new BigDecimal("80000.00"),
                710,
                false,
                0,
                new BigDecimal("3000000.00"),
                12
        );

        DecisionResult result = pipeline.evaluate(input);

        assertEquals(DecisionStatus.COUNTER_OFFER, result.status());
        assertEquals(RiskGrade.GRADE_B, result.riskGrade());
        assertEquals(
                List.of(DecisionReasonCode.PAYMENT_CAPACITY_LIMITED_ON_REQUESTED_TERM),
                result.reasonCodes()
        );
        assertEquals(2, result.offers().size());

        LoanOffer reducedAmountOffer = result.offers().get(0);
        assertEquals(OfferType.REDUCED_AMOUNT_SAME_TERM, reducedAmountOffer.offerType());
        assertEquals(new BigDecimal("1560000.00"), reducedAmountOffer.principalAmount());
        assertEquals(12, reducedAmountOffer.termMonths());

        LoanOffer extendedTermOffer = result.offers().get(1);
        assertEquals(OfferType.EXTENDED_TERM_FOR_AMOUNT, extendedTermOffer.offerType());
        assertEquals(new BigDecimal("3000000.00"), extendedTermOffer.principalAmount());
        assertEquals(36, extendedTermOffer.termMonths());
        assertTrue(extendedTermOffer.monthlyPayment().compareTo(new BigDecimal("145000.00")) <= 0);
    }

    @Test
    void shouldApplyRiskGradeCapWhenRequestedAmountExceedsGradeLimit() {
        // Score 550 -> GRADE_D (32% APR, 1,000,000 KZT cap)
        // High income 1,000,000 KZT, but requests 2,000,000 KZT on 24 months
        DecisionInput input = new DecisionInput(
                35,
                new BigDecimal("1000000.00"),
                new BigDecimal("0.00"),
                550,
                false,
                0,
                new BigDecimal("2000000.00"),
                24
        );

        DecisionResult result = pipeline.evaluate(input);

        assertEquals(DecisionStatus.COUNTER_OFFER, result.status());
        assertEquals(RiskGrade.GRADE_D, result.riskGrade());
        assertEquals(List.of(DecisionReasonCode.RISK_GRADE_CAP_APPLIED), result.reasonCodes());
        assertEquals(1, result.offers().size());
        assertEquals(new BigDecimal("1000000.00"), result.offers().getFirst().principalAmount());
    }

    @Test
    void shouldRejectWhenHardStopRuleTriggered() {
        DecisionInput input = new DecisionInput(
                19,
                new BigDecimal("500000.00"),
                new BigDecimal("0.00"),
                750,
                true,
                0,
                new BigDecimal("1000000.00"),
                12
        );

        DecisionResult result = pipeline.evaluate(input);

        assertEquals(DecisionStatus.REJECTED, result.status());
        assertEquals(
                List.of(
                        DecisionReasonCode.APPLICANT_TOO_YOUNG,
                        DecisionReasonCode.ACTIVE_DELINQUENCY_PRESENT
                ),
                result.reasonCodes()
        );
        assertTrue(result.offers().isEmpty());
    }

    @Test
    void shouldRejectWhenMaxAffordablePrincipalBelowMinimumProductThreshold() {
        // Income 100,000; Existing 10,000 -> Subsistence budget = 100,000 - 85,000 - 10,000 = 5,000 KZT
        // On 6 months at 32% APR, 5,000 KZT budget yields < 100,000 KZT minimum product amount
        DecisionInput input = new DecisionInput(
                64,
                new BigDecimal("100000.00"),
                new BigDecimal("10000.00"),
                540,
                false,
                0,
                new BigDecimal("500000.00"),
                6
        );

        DecisionResult result = pipeline.evaluate(input);

        assertEquals(DecisionStatus.REJECTED, result.status());
        assertEquals(
                List.of(DecisionReasonCode.CAPACITY_BELOW_MIN_PRODUCT_AMOUNT),
                result.reasonCodes()
        );
        assertTrue(result.offers().isEmpty());
    }
}
