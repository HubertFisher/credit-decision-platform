package kz.alitulegenov.creditdecisionengine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kz.alitulegenov.creditdecisionengine.domain.model.DecisionInput;
import kz.alitulegenov.creditdecisionengine.domain.model.DecisionReasonCode;
import kz.alitulegenov.creditdecisionengine.domain.model.DecisionResult;
import kz.alitulegenov.creditdecisionengine.domain.model.DecisionStatus;
import kz.alitulegenov.creditdecisionengine.domain.model.LoanOffer;
import kz.alitulegenov.creditdecisionengine.domain.model.OfferType;
import kz.alitulegenov.creditdecisionengine.domain.offer.DecisionPipeline;
import kz.alitulegenov.creditdecisionengine.domain.policy.RiskGrade;
import kz.alitulegenov.creditdecisionengine.dto.CreditDecisionRequest;
import kz.alitulegenov.creditdecisionengine.dto.CreditDecisionResponse;
import kz.alitulegenov.creditdecisionengine.entity.CreditDecision;
import kz.alitulegenov.creditdecisionengine.exception.CreditDecisionNotFoundException;
import kz.alitulegenov.creditdecisionengine.repository.CreditDecisionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreditDecisionServiceTest {

    @Mock
    private CreditDecisionRepository repository;

    @Mock
    private DecisionPipeline decisionPipeline;

    @InjectMocks
    private CreditDecisionService service;

    @Test
    void shouldReturnExistingDecisionWithoutRecomputingWhenRequestIdAlreadyExists() {
        UUID requestId = UUID.randomUUID();
        CreditDecisionRequest request = sampleRequest(requestId);
        CreditDecision existingEntity = sampleEntity(requestId, DecisionStatus.APPROVED);

        when(repository.findByRequestId(requestId)).thenReturn(Optional.of(existingEntity));

        CreditDecisionResponse response = service.evaluateAndSave(request);

        assertEquals(requestId, response.requestId());
        assertEquals(DecisionStatus.APPROVED, response.status());
        verifyNoInteractions(decisionPipeline);
        verify(repository, never()).save(any());
    }

    @Test
    void shouldEvaluatePipelineAndPersistDecisionWhenRequestIdIsNew() {
        UUID requestId = UUID.randomUUID();
        CreditDecisionRequest request = sampleRequest(requestId);

        LoanOffer approvedOffer = new LoanOffer(
                OfferType.REQUESTED_TERMS,
                new BigDecimal("2000000.00"),
                24,
                new BigDecimal("0.1600"),
                new BigDecimal("97926.22"),
                new BigDecimal("350229.28"),
                new BigDecimal("2350229.28"),
                new BigDecimal("0.2959")
        );

        DecisionResult pipelineResult = new DecisionResult(
                DecisionStatus.APPROVED,
                RiskGrade.GRADE_A,
                new BigDecimal("0.1600"),
                new BigDecimal("10000000.00"),
                new BigDecimal("0.1000"),
                new BigDecimal("200000.00"),
                List.of(DecisionReasonCode.APPROVED_AS_REQUESTED),
                List.of(approvedOffer)
        );

        when(repository.findByRequestId(requestId)).thenReturn(Optional.empty());
        when(decisionPipeline.evaluate(any(DecisionInput.class))).thenReturn(pipelineResult);
        when(repository.save(any(CreditDecision.class))).thenAnswer(inv -> inv.getArgument(0));

        CreditDecisionResponse response = service.evaluateAndSave(request);

        assertEquals(requestId, response.requestId());
        assertEquals(DecisionStatus.APPROVED, response.status());
        assertEquals(RiskGrade.GRADE_A, response.riskGrade());
        assertEquals(1, response.offers().size());
        verify(decisionPipeline).evaluate(any(DecisionInput.class));
        verify(repository).save(any(CreditDecision.class));
    }

    @Test
    void shouldThrowNotFoundExceptionWhenDecisionDoesNotExist() {
        UUID unknownId = UUID.randomUUID();
        when(repository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(CreditDecisionNotFoundException.class, () -> service.getById(unknownId));
    }

    private CreditDecisionRequest sampleRequest(UUID requestId) {
        return new CreditDecisionRequest(
                requestId,
                32,
                new BigDecimal("500000.00"),
                new BigDecimal("50000.00"),
                780,
                false,
                0,
                new BigDecimal("2000000.00"),
                24
        );
    }

    private CreditDecision sampleEntity(UUID requestId, DecisionStatus status) {
        return new CreditDecision(
                requestId,
                status,
                32,
                new BigDecimal("500000.00"),
                new BigDecimal("50000.00"),
                780,
                false,
                0,
                new BigDecimal("2000000.00"),
                24,
                RiskGrade.GRADE_A,
                new BigDecimal("0.1600"),
                new BigDecimal("10000000.00"),
                new BigDecimal("0.1000"),
                new BigDecimal("200000.00"),
                List.of(DecisionReasonCode.APPROVED_AS_REQUESTED),
                List.of()
        );
    }
}
