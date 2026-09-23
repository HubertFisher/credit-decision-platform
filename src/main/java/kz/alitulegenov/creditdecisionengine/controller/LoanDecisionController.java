package kz.alitulegenov.creditdecisionengine.controller;

import jakarta.validation.Valid;
import kz.alitulegenov.creditdecisionengine.dto.LoanDecisionRequest;
import kz.alitulegenov.creditdecisionengine.dto.LoanDecisionResponse;
import kz.alitulegenov.creditdecisionengine.service.LoanDecisionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


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

}
