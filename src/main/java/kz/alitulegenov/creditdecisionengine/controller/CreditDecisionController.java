package kz.alitulegenov.creditdecisionengine.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import kz.alitulegenov.creditdecisionengine.dto.CreditDecisionPageResponse;
import kz.alitulegenov.creditdecisionengine.dto.CreditDecisionRequest;
import kz.alitulegenov.creditdecisionengine.dto.CreditDecisionResponse;
import kz.alitulegenov.creditdecisionengine.service.CreditDecisionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing credit decision evaluation and audit history endpoints.
 */
@RestController
@RequestMapping("/api/decisions")
public class CreditDecisionController {

    private final CreditDecisionService creditDecisionService;

    public CreditDecisionController(CreditDecisionService creditDecisionService) {
        this.creditDecisionService = creditDecisionService;
    }

    /**
     * Evaluates a borrower profile through the decision pipeline and records the decision idempotently.
     */
    @PostMapping
    public CreditDecisionResponse evaluateDecision(
        @Valid @RequestBody CreditDecisionRequest request
    ) {
        return creditDecisionService.evaluateAndSave(request);
    }

    /**
     * Retrieves a previously recorded credit decision by its internal UUID.
     */
    @GetMapping("/{id}")
    public CreditDecisionResponse getDecisionById(@PathVariable UUID id) {
        return creditDecisionService.getById(id);
    }

    /**
     * Returns a paginated history of recorded credit decisions ordered from newest to oldest.
     */
    @GetMapping
    public CreditDecisionPageResponse getDecisions(
        @RequestParam(defaultValue = "0")
        @Min(value = 0, message = "Page index must not be less than zero")
        int page,
        @RequestParam(defaultValue = "20")
        @Min(value = 1, message = "Page size must be at least 1")
        @Max(value = 100, message = "Page size must not be greater than 100")
        int size
    ) {
        return creditDecisionService.getDecisions(page, size);
    }
}
