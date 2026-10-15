package kz.alitulegenov.creditdecisionengine.risk;

public record ScoringRuleResult(
        ScoringRuleCode code,
        boolean applied,
        int points
) {
}
