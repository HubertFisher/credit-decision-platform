package kz.alitulegenov.creditdecisionengine.domain.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import kz.alitulegenov.creditdecisionengine.domain.model.DecisionInput;
import kz.alitulegenov.creditdecisionengine.domain.model.DecisionReasonCode;
import kz.alitulegenov.creditdecisionengine.domain.policy.CreditPolicy;
import org.junit.jupiter.api.Test;

class HardStopEvaluatorTest {

    private final HardStopEvaluator evaluator = new HardStopEvaluator();
    private final CreditPolicy policy = CreditPolicy.defaultPolicy();

    @Test
    void shouldReturnEmptyListWhenAllCriteriaPass() {
        DecisionInput input = validInput(21, 520, false, 30);

        List<DecisionReasonCode> reasons = evaluator.evaluate(input, policy);

        assertTrue(reasons.isEmpty());
    }

    @Test
    void shouldRejectWhenApplicantTooYoung() {
        DecisionInput input = validInput(20, 700, false, 0);

        List<DecisionReasonCode> reasons = evaluator.evaluate(input, policy);

        assertEquals(List.of(DecisionReasonCode.APPLICANT_TOO_YOUNG), reasons);
    }

    @Test
    void shouldRejectWhenAgeAtMinimumMaturityExceedsLimit() {
        DecisionInput age64 = validInput(64, 700, false, 0);
        DecisionInput age65 = validInput(65, 700, false, 0);

        assertTrue(evaluator.evaluate(age64, policy).isEmpty());
        assertEquals(
                List.of(DecisionReasonCode.AGE_AT_MATURITY_EXCEEDED),
                evaluator.evaluate(age65, policy)
        );
    }

    @Test
    void shouldRejectWhenActiveDelinquencyPresent() {
        DecisionInput input = validInput(30, 700, true, 0);

        List<DecisionReasonCode> reasons = evaluator.evaluate(input, policy);

        assertEquals(List.of(DecisionReasonCode.ACTIVE_DELINQUENCY_PRESENT), reasons);
    }

    @Test
    void shouldRejectWhenHistoricalDelinquencyExceeds30Days() {
        DecisionInput input = validInput(30, 700, false, 31);

        List<DecisionReasonCode> reasons = evaluator.evaluate(input, policy);

        assertEquals(List.of(DecisionReasonCode.SEVERE_HISTORICAL_DELINQUENCY), reasons);
    }

    @Test
    void shouldRejectWhenCreditScoreBelow520() {
        DecisionInput input = validInput(30, 519, false, 0);

        List<DecisionReasonCode> reasons = evaluator.evaluate(input, policy);

        assertEquals(List.of(DecisionReasonCode.CREDIT_SCORE_BELOW_MINIMUM), reasons);
    }

    @Test
    void shouldCollectAllReasonsAndReturnImmutableList() {
        DecisionInput input = validInput(19, 450, true, 60);

        List<DecisionReasonCode> reasons = evaluator.evaluate(input, policy);

        assertEquals(
                List.of(
                        DecisionReasonCode.APPLICANT_TOO_YOUNG,
                        DecisionReasonCode.ACTIVE_DELINQUENCY_PRESENT,
                        DecisionReasonCode.SEVERE_HISTORICAL_DELINQUENCY,
                        DecisionReasonCode.CREDIT_SCORE_BELOW_MINIMUM
                ),
                reasons
        );
        assertThrows(UnsupportedOperationException.class, () -> reasons.add(DecisionReasonCode.DTI_LIMIT_EXCEEDED));
    }

    private DecisionInput validInput(int age, int creditScore, boolean activeDelinquency, int maxDelinquencyDays) {
        return new DecisionInput(
                age,
                new BigDecimal("500000.00"),
                new BigDecimal("50000.00"),
                creditScore,
                activeDelinquency,
                maxDelinquencyDays,
                new BigDecimal("1000000.00"),
                12
        );
    }
}
