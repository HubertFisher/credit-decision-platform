package kz.alitulegenov.creditdecisionengine.service;

import kz.alitulegenov.creditdecisionengine.dto.LoanDecisionRequest;
import kz.alitulegenov.creditdecisionengine.dto.LoanDecisionResponse;
import kz.alitulegenov.creditdecisionengine.dto.LoanDecisionPageResponse;
import kz.alitulegenov.creditdecisionengine.entity.LoanDecision;
import kz.alitulegenov.creditdecisionengine.exception.LoanDecisionNotFoundException;
import kz.alitulegenov.creditdecisionengine.repository.LoanDecisionRepository;
import kz.alitulegenov.creditdecisionengine.risk.LoanRiskCalculator;
import kz.alitulegenov.creditdecisionengine.risk.ScoringInput;
import kz.alitulegenov.creditdecisionengine.risk.ScoringResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class LoanDecisionService {

    private final LoanRiskCalculator loanRiskCalculator;
    private final LoanDecisionRepository loanDecisionRepository;

    public LoanDecisionService(
            LoanRiskCalculator loanRiskCalculator,
            LoanDecisionRepository loanDecisionRepository
    ) {
        this.loanRiskCalculator = loanRiskCalculator;
        this.loanDecisionRepository = loanDecisionRepository;
    }

    public LoanDecisionResponse makeDecision(LoanDecisionRequest request) {
        ScoringInput input = new ScoringInput(
                request.income(),
                request.requestedAmount(),
                request.termMonths()
        );

        ScoringResult result = loanRiskCalculator.calculateRisk(input);

        boolean approved = result.riskScore() < 50;

        LoanDecision decision = new LoanDecision(
                request.income(),
                request.requestedAmount(),
                request.termMonths(),
                result.riskScore(),
                approved
        );

        LoanDecision savedDecision = loanDecisionRepository.save(decision);
        return toResponse(savedDecision);
    }

    public LoanDecisionResponse getLoanDecisionResponseById(UUID id) {
        LoanDecision decision = loanDecisionRepository.findById(id)
                .orElseThrow(() -> new LoanDecisionNotFoundException(id));

        return toResponse(decision);
    }

    public LoanDecisionPageResponse getLoanDecisionPageResponse(int page, int pageSize) {
        Pageable pageable = PageRequest.of(
                page,
                pageSize,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );
        Page<LoanDecision> decisions = loanDecisionRepository.findAll(pageable);
        Page<LoanDecisionResponse> responses = decisions.map(this::toResponse);

        return new LoanDecisionPageResponse(
                responses.getContent(),
                responses.getNumber(),
                responses.getSize(),
                responses.getTotalElements(),
                responses.getTotalPages()
        );
    }

    private LoanDecisionResponse toResponse(LoanDecision decision) {
        return new LoanDecisionResponse(
                decision.getId(),
                decision.isApproved(),
                decision.getIncome(),
                decision.getRequestedAmount(),
                decision.getTermMonths(),
                decision.getRiskScore(),
                decision.getCreatedAt()
        );
    }
}
