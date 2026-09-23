package kz.alitulegenov.creditdecisionengine.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record LoanDecisionRequest (
        @NotNull(message = "Indicate income")
        @Positive(message = "Income must be greater than zero")
        BigDecimal income,

        @NotNull(message = "Indicate requested amount")
        @Positive(message = "Requested amount must be greater than zero")
        BigDecimal requestedAmount,

        @NotNull(message = "Indicate term months")
        @Positive(message = "Loan terms must be greater than zero")
        @Max(value = 360, message = "Loan term must not exceed 360 months")
        Integer termMonths
) {}
