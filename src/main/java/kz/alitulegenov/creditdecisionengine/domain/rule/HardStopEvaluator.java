package kz.alitulegenov.creditdecisionengine.domain.rule;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import kz.alitulegenov.creditdecisionengine.domain.model.DecisionInput;
import kz.alitulegenov.creditdecisionengine.domain.model.DecisionReasonCode;
import kz.alitulegenov.creditdecisionengine.domain.policy.CreditPolicy;
import org.springframework.stereotype.Component;

/**
 * Stage 1 of the Decision Pipeline: Hard Stop (Knock-out) Rule Evaluator.
 *
 * <p>Screens the applicant against mandatory eligibility criteria (age boundaries, active and
 * historical credit bureau delinquencies, and minimum credit score cut-off) before executing
 * pricing or financial calculations. All rules are evaluated exhaustively so that every applicable
 * rejection reason is recorded in the audit snapshot.
 */
@Component
public class HardStopEvaluator {

    private static final int MONTHS_IN_YEAR = 12;

    /**
     * Evaluates all hard-stop rules against the given borrower profile.
     *
     * @param input  normalized borrower and loan request profile
     * @param policy active credit policy thresholds
     * @return immutable list of triggered rejection reason codes (empty if the applicant passes Stage 1)
     */
    public List<DecisionReasonCode> evaluate(DecisionInput input, CreditPolicy policy) {
        Objects.requireNonNull(input, "input must not be null");
        Objects.requireNonNull(policy, "policy must not be null");

        List<DecisionReasonCode> reasons = new ArrayList<>();

        // 1. Minimum age check at the time of application
        if (input.applicantAgeYears() < policy.minAgeAtApplication()) {
            reasons.add(DecisionReasonCode.APPLICANT_TOO_YOUNG);
        }

        // 2. Maximum age check at maturity on the shortest possible product term.
        // If the applicant fits the shortest term but exceeds 65 on the requested term,
        // Stage 5 will generate a shortened-term counter-offer instead of a hard rejection.
        int minMaturityYears = input.applicantAgeYears() + ceilYears(policy.minTermMonths());
        if (minMaturityYears > policy.maxAgeAtMaturity()) {
            reasons.add(DecisionReasonCode.AGE_AT_MATURITY_EXCEEDED);
        }

        // 3. Zero tolerance for currently active overdue debt in credit bureau
        if (input.hasActiveDelinquency()) {
            reasons.add(DecisionReasonCode.ACTIVE_DELINQUENCY_PRESENT);
        }

        // 4. Historical delinquency depth check over the past 12 months
        if (input.maxDelinquencyDaysLast12Months() > policy.maxAllowedDelinquencyDays()) {
            reasons.add(DecisionReasonCode.SEVERE_HISTORICAL_DELINQUENCY);
        }

        // 5. Minimum credit bureau score cut-off
        if (input.creditScore() < policy.minCreditScore()) {
            reasons.add(DecisionReasonCode.CREDIT_SCORE_BELOW_MINIMUM);
        }

        return List.copyOf(reasons);
    }

    /**
     * Converts a loan term in months into full years rounded up (ceiling integer division).
     */
    private int ceilYears(int termMonths) {
        return (termMonths + MONTHS_IN_YEAR - 1) / MONTHS_IN_YEAR;
    }
}
