package kz.alitulegenov.creditdecisionengine.service;

import kz.alitulegenov.creditdecisionengine.dto.LoanDecisionPageResponse;
import kz.alitulegenov.creditdecisionengine.dto.LoanDecisionRequest;
import kz.alitulegenov.creditdecisionengine.dto.LoanDecisionResponse;
import kz.alitulegenov.creditdecisionengine.entity.LoanDecision;
import kz.alitulegenov.creditdecisionengine.exception.LoanDecisionNotFoundException;
import kz.alitulegenov.creditdecisionengine.repository.LoanDecisionRepository;
import kz.alitulegenov.creditdecisionengine.risk.LoanRiskCalculator;
import kz.alitulegenov.creditdecisionengine.risk.ScoringInput;
import kz.alitulegenov.creditdecisionengine.risk.ScoringResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LoanDecisionServiceTest {

    private final LoanRiskCalculator calculator =
            mock(LoanRiskCalculator.class);

    private final LoanDecisionRepository repository =
            mock(LoanDecisionRepository.class);

    private final LoanDecisionService service =
            new LoanDecisionService(calculator, repository);

    @Test
    void shouldThrowWhenDecisionDoesNotExist() {
        UUID uuid = UUID.randomUUID();

        when(repository.findById(uuid))
                .thenReturn(Optional.empty());

        assertThrows(
                LoanDecisionNotFoundException.class,
                () -> service.getLoanDecisionResponseById(uuid)
        );
    }

    @Test
    void shouldReturnDecisionWhenItExists() {
        UUID uuid = UUID.randomUUID();

        LoanDecision decision = new LoanDecision(
                new BigDecimal("500000"),
                new BigDecimal("3000000"),
                24,
                0,
                true
        );

        ReflectionTestUtils.setField(decision, "id", uuid);

        when(repository.findById(uuid))
                .thenReturn(Optional.of(decision));

        LoanDecisionResponse response =
                service.getLoanDecisionResponseById(uuid);

        assertEquals(uuid, response.id());
        assertEquals(decision.isApproved(), response.approved());
        assertEquals(decision.getIncome(), response.income());
        assertEquals(decision.getRequestedAmount(), response.requestedAmount());
        assertEquals(decision.getTermMonths().intValue(), response.termMonths());
        assertEquals(decision.getRiskScore(), response.riskScore());
        assertEquals(decision.getCreatedAt(), response.createdAt());
    }

    @Test
    void shouldRejectDecisionWhenRiskIsExactlyFifty() {
        LoanDecisionRequest request = new LoanDecisionRequest(
                new BigDecimal("500000"),
                new BigDecimal("3000000"),
                24
        );

        UUID uuid = UUID.randomUUID();

        LoanDecision savedDecision = new LoanDecision(
                request.income(),
                request.requestedAmount(),
                request.termMonths(),
                50,
                false
        );

        ReflectionTestUtils.setField(savedDecision, "id", uuid);

        ScoringInput expectedInput = new ScoringInput(
                new BigDecimal("500000"),
                new BigDecimal("3000000"),
                24
        );

        when(calculator.calculateRisk(expectedInput))
                .thenReturn(new ScoringResult("legacy-v1", 50, List.of()));
        when(repository.save(any(LoanDecision.class)))
                .thenReturn(savedDecision);

        LoanDecisionResponse response = service.makeDecision(request);

        verify(calculator).calculateRisk(expectedInput);

        ArgumentCaptor<LoanDecision> captor =
                ArgumentCaptor.forClass(LoanDecision.class);
        verify(repository).save(captor.capture());
        LoanDecision capturedDecision = captor.getValue();

        assertEquals(request.income(), capturedDecision.getIncome());
        assertEquals(request.requestedAmount(), capturedDecision.getRequestedAmount());
        assertEquals(request.termMonths(), capturedDecision.getTermMonths());
        assertEquals(50, capturedDecision.getRiskScore());
        assertFalse(capturedDecision.isApproved());

        assertEquals(uuid, response.id());
        assertEquals(50, response.riskScore());
        assertFalse(response.approved());
    }

    @Test
    void shouldApproveDecisionWhenRiskIsBelowFifty() {
        LoanDecisionRequest request = new LoanDecisionRequest(
                new BigDecimal("500000"),
                new BigDecimal("3000000"),
                24
        );

        UUID uuid = UUID.randomUUID();

        LoanDecision savedDecision = new LoanDecision(
                request.income(),
                request.requestedAmount(),
                request.termMonths(),
                49,
                true
        );

        ReflectionTestUtils.setField(savedDecision, "id", uuid);

        ScoringInput expectedInput = new ScoringInput(
                new BigDecimal("500000"),
                new BigDecimal("3000000"),
                24
        );

        when(calculator.calculateRisk(expectedInput))
                .thenReturn(new ScoringResult("legacy-v1", 49, List.of()));
        when(repository.save(any(LoanDecision.class)))
                .thenReturn(savedDecision);

        LoanDecisionResponse response = service.makeDecision(request);

        verify(calculator).calculateRisk(expectedInput);

        ArgumentCaptor<LoanDecision> captor =
                ArgumentCaptor.forClass(LoanDecision.class);
        verify(repository).save(captor.capture());
        LoanDecision capturedDecision = captor.getValue();

        assertEquals(request.income(), capturedDecision.getIncome());
        assertEquals(request.requestedAmount(), capturedDecision.getRequestedAmount());
        assertEquals(request.termMonths(), capturedDecision.getTermMonths());
        assertEquals(49, capturedDecision.getRiskScore());
        assertTrue(capturedDecision.isApproved());

        assertEquals(uuid, response.id());
        assertEquals(49, response.riskScore());
        assertTrue(response.approved());
    }

    @Test
    void shouldReturnDecisionPageWithCorrectMetadata() {
        UUID id = UUID.randomUUID();

        LoanDecision decision = new LoanDecision(
                new BigDecimal("500000"),
                new BigDecimal("3000000"),
                24,
                0,
                true
        );

        ReflectionTestUtils.setField(decision, "id", id);

        Page<LoanDecision> repositoryPage = new PageImpl<>(
                List.of(decision),
                PageRequest.of(1, 3),
                4
        );

        when(repository.findAll(any(Pageable.class)))
                .thenReturn(repositoryPage);

        LoanDecisionPageResponse response =
                service.getLoanDecisionPageResponse(1, 3);

        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(Pageable.class);

        verify(repository).findAll(captor.capture());

        Pageable requestedPage = captor.getValue();

        assertEquals(1, requestedPage.getPageNumber());
        assertEquals(3, requestedPage.getPageSize());
        assertEquals(
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                ),
                requestedPage.getSort()
        );

        assertEquals(1, response.items().size());
        assertEquals(id, response.items().getFirst().id());
        assertEquals(decision.getIncome(), response.items().getFirst().income());
        assertEquals(1, response.page());
        assertEquals(3, response.size());
        assertEquals(4L, response.totalElements());
        assertEquals(2, response.totalPages());
    }
}
