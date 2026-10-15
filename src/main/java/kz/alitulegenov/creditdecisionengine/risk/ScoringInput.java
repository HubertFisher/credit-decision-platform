package kz.alitulegenov.creditdecisionengine.risk;

import java.math.BigDecimal;

public record ScoringInput(
        BigDecimal monthlyIncome,
        BigDecimal requestedAmount,
        int termMonths
) {
}
