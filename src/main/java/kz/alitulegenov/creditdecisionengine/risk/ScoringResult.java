package kz.alitulegenov.creditdecisionengine.risk;

import java.util.List;

public record ScoringResult(
        String policyVersion,
        int riskScore,
        List<ScoringRuleResult> breakdown
) {

    public ScoringResult {
        breakdown = List.copyOf(breakdown);
    }
}
