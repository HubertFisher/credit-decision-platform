package kz.alitulegenov.creditdecisionengine.dto;

import java.time.Instant;
import java.util.UUID;

public record LoanDecisionResponse (
        UUID id,
        boolean approved,
        int riskScore,
        Instant createdAt
) {}
