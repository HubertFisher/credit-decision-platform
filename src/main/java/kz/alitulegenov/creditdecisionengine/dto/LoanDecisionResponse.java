package kz.alitulegenov.creditdecisionengine.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LoanDecisionResponse(
        UUID id,
        boolean approved,
        BigDecimal income,
        BigDecimal requestedAmount,
        int termMonths,
        int riskScore,
        Instant createdAt
) {}
