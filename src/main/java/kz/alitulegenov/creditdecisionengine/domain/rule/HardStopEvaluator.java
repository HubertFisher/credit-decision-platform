package kz.alitulegenov.creditdecisionengine.domain.rule;

import java.util.ArrayList;
import java.util.List;
import kz.alitulegenov.creditdecisionengine.domain.model.DecisionInput;
import kz.alitulegenov.creditdecisionengine.domain.model.DecisionReasonCode;
import kz.alitulegenov.creditdecisionengine.domain.policy.CreditPolicy;
import org.springframework.stereotype.Component;

@Component
public class HardStopEvaluator {

    public List<DecisionReasonCode> evaluate(DecisionInput input, CreditPolicy policy) {
        List<DecisionReasonCode> reasons = new ArrayList<>();

        if (input.applicantAgeYears() < policy.minAgeAtApplication()) {
            reasons.add(DecisionReasonCode.APPLICANT_TOO_YOUNG);
        }

        int minMaturityYears = input.applicantAgeYears() + ceilYears(policy.minTermMonths());
        if (minMaturityYears > policy.maxAgeAtMaturity()) {
            reasons.add(DecisionReasonCode.AGE_AT_MATURITY_EXCEEDED);
        }

        if (input.hasActiveDelinquency()) {
            reasons.add(DecisionReasonCode.ACTIVE_DELINQUENCY_PRESENT);
        }

        if (input.maxDelinquencyDaysLast12Months() > policy.maxAllowedDelinquencyDays()) {
            reasons.add(DecisionReasonCode.SEVERE_HISTORICAL_DELINQUENCY);
        }

        if (input.creditScore() < policy.minCreditScore()) {
            reasons.add(DecisionReasonCode.CREDIT_SCORE_BELOW_MINIMUM);
        }

        return List.copyOf(reasons);
    }

    private int ceilYears(int termMonths) {
        return (termMonths + 11) / 12;
    }
}
