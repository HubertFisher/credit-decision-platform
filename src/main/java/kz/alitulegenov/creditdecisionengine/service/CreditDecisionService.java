package kz.alitulegenov.creditdecisionengine.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import kz.alitulegenov.creditdecisionengine.domain.model.DecisionInput;
import kz.alitulegenov.creditdecisionengine.domain.model.DecisionResult;
import kz.alitulegenov.creditdecisionengine.domain.offer.DecisionPipeline;
import kz.alitulegenov.creditdecisionengine.dto.CreditDecisionPageResponse;
import kz.alitulegenov.creditdecisionengine.dto.CreditDecisionRequest;
import kz.alitulegenov.creditdecisionengine.dto.CreditDecisionResponse;
import kz.alitulegenov.creditdecisionengine.entity.CreditDecision;
import kz.alitulegenov.creditdecisionengine.exception.CreditDecisionNotFoundException;
import kz.alitulegenov.creditdecisionengine.repository.CreditDecisionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service coordinating idempotency checks, domain pipeline execution,
 * and audit snapshot persistence for credit decisions.
 */
@Service
public class CreditDecisionService {

    private static final Sort DEFAULT_HISTORY_SORT = Sort.by(
        Sort.Order.desc("createdAt"),
        Sort.Order.desc("id")
    );

    private final CreditDecisionRepository repository;
    private final DecisionPipeline decisionPipeline;

    public CreditDecisionService(
        CreditDecisionRepository repository,
        DecisionPipeline decisionPipeline
    ) {
        this.repository = repository;
        this.decisionPipeline = decisionPipeline;
    }

    /**
     * Evaluates a credit decision request idempotently by {@code requestId} and persists
     * the resulting audit snapshot.
     *
     * <p>If a decision with the same {@code requestId} has already been recorded, the existing
     * snapshot is returned immediately without re-running the pipeline.
     */
    @Transactional
    public CreditDecisionResponse evaluateAndSave(CreditDecisionRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        Optional<CreditDecision> existing = repository.findByRequestId(request.requestId());
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        DecisionInput input = new DecisionInput(
            request.applicantAgeYears(),
            request.monthlyIncome(),
            request.existingMonthlyPayments(),
            request.creditScore(),
            request.hasActiveDelinquency(),
            request.maxDelinquencyDaysLast12Months(),
            request.requestedAmount(),
            request.requestedTermMonths()
        );

        DecisionResult result = decisionPipeline.evaluate(input);

        CreditDecision entity = new CreditDecision(
            request.requestId(),
            result.status(),
            input.applicantAgeYears(),
            input.monthlyIncome(),
            input.existingMonthlyPayments(),
            input.creditScore(),
            input.hasActiveDelinquency(),
            input.maxDelinquencyDaysLast12Months(),
            input.requestedAmount(),
            input.requestedTermMonths(),
            result.riskGrade(),
            result.assignedAnnualRate(),
            result.riskExposureCap(),
            result.currentDtiRatio(),
            result.availableMonthlyBudget(),
            result.reasonCodes(),
            result.offers()
        );

        CreditDecision saved = repository.save(entity);
        return toResponse(saved);
    }

    /**
     * Retrieves a single recorded credit decision by its internal UUID.
     */
    @Transactional(readOnly = true)
    public CreditDecisionResponse getById(UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        CreditDecision decision = repository.findById(id)
            .orElseThrow(() -> new CreditDecisionNotFoundException(id));
        return toResponse(decision);
    }

    /**
     * Retrieves a paginated slice of recorded credit decisions ordered by {@code createdAt DESC, id DESC}.
     */
    @Transactional(readOnly = true)
    public CreditDecisionPageResponse getDecisions(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, DEFAULT_HISTORY_SORT);
        Page<CreditDecision> decisionPage = repository.findAll(pageable);

        List<CreditDecisionResponse> items = decisionPage.getContent()
            .stream()
            .map(this::toResponse)
            .toList();

        return new CreditDecisionPageResponse(
            items,
            decisionPage.getNumber(),
            decisionPage.getSize(),
            decisionPage.getTotalElements(),
            decisionPage.getTotalPages()
        );
    }

    private CreditDecisionResponse toResponse(CreditDecision entity) {
        return new CreditDecisionResponse(
            entity.getId(),
            entity.getRequestId(),
            entity.getStatus(),
            entity.getApplicantAgeYears(),
            entity.getMonthlyIncome(),
            entity.getExistingMonthlyPayments(),
            entity.getCreditScore(),
            entity.isHasActiveDelinquency(),
            entity.getMaxDelinquencyDaysLast12Months(),
            entity.getRequestedAmount(),
            entity.getRequestedTermMonths(),
            entity.getRiskGrade(),
            entity.getAssignedAnnualRate(),
            entity.getRiskExposureCap(),
            entity.getCurrentDtiRatio(),
            entity.getAvailableMonthlyBudget(),
            entity.getReasonCodes(),
            entity.getOffers(),
            entity.getCreatedAt()
        );
    }
}
