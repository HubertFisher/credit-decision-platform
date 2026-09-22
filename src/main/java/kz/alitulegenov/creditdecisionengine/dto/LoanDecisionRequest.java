package kz.alitulegenov.creditdecisionengine.dto;

import java.math.BigDecimal;

public record LoanDecisionRequest (
        BigDecimal income,
        BigDecimal requestedAmount,
        Integer termMonths
) {}
