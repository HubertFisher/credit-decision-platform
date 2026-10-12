package kz.alitulegenov.creditdecisionengine.dto;

import java.util.List;

public record LoanDecisionPageResponse(
        List<LoanDecisionResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
