package kz.alitulegenov.creditdecisionengine.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import kz.alitulegenov.creditdecisionengine.dto.LoanDecisionPageResponse;
import kz.alitulegenov.creditdecisionengine.dto.LoanDecisionRequest;
import kz.alitulegenov.creditdecisionengine.dto.LoanDecisionResponse;
import kz.alitulegenov.creditdecisionengine.service.LoanDecisionService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/decisions")
public class LoanDecisionController {

    private final LoanDecisionService loanDecisionService;

    public LoanDecisionController(LoanDecisionService loanDecisionService) {
        this.loanDecisionService = loanDecisionService;
    }

    @PostMapping
    public LoanDecisionResponse makeDecision(
            @Valid @RequestBody LoanDecisionRequest loanDecisionRequest
    ) {
        return loanDecisionService.makeDecision(loanDecisionRequest);
    }

    @GetMapping("/{uuid}")
    public LoanDecisionResponse getDecisions(@PathVariable UUID uuid) {
        return loanDecisionService.getLoanDecisionResponseById(uuid);
    }

    @GetMapping
    public LoanDecisionPageResponse getDecisions(
            @RequestParam(defaultValue = "0")
            @Min(0)
            int page,

            @RequestParam(defaultValue = "20")
            @Min(1)
            @Max(100)
            int size
    ) {
        return loanDecisionService.getLoanDecisionPageResponse(page, size);
    }
}
