package kz.alitulegenov.creditdecisionengine.service;

import kz.alitulegenov.creditdecisionengine.dto.LoanDecisionRequest;
import kz.alitulegenov.creditdecisionengine.dto.LoanDecisionResponse;
import kz.alitulegenov.creditdecisionengine.entity.LoanDecision;
import kz.alitulegenov.creditdecisionengine.repository.LoanDecisionRepository;
import kz.alitulegenov.creditdecisionengine.risk.LoanRiskCalculator;
import org.springframework.stereotype.Service;

@Service
public class LoanDecisionService {

    private final LoanRiskCalculator loanRiskCalculator;
    private final LoanDecisionRepository loanDecisionRepository;

    public LoanDecisionService(LoanRiskCalculator loanRiskCalculator, LoanDecisionRepository loanDecisionRepository) {
        this.loanRiskCalculator = loanRiskCalculator;
        this.loanDecisionRepository = loanDecisionRepository;
    }

    public LoanDecisionResponse makeDecision(LoanDecisionRequest request) {
       int risk = loanRiskCalculator.calculateRisk(request);
       boolean approved = risk < 50;
       LoanDecision decision = loanDecisionRepository.save(new LoanDecision(request.income(), request.requestedAmount(), request.termMonths(), risk, approved));
       return new LoanDecisionResponse(decision.getId(),decision.isApproved(), decision.getRiskScore());
    }
}
