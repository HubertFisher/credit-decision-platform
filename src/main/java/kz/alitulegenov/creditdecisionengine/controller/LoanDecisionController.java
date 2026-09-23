package kz.alitulegenov.creditdecisionengine.controller;

import jakarta.validation.Valid;
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
    public LoanDecisionResponse getDecisions(@PathVariable UUID uuid){
        return loanDecisionService.getLoanDecisionResponseById(uuid);
    }
}
