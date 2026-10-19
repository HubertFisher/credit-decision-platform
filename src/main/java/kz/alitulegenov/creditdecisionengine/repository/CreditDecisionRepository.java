package kz.alitulegenov.creditdecisionengine.repository;

import java.util.Optional;
import java.util.UUID;
import kz.alitulegenov.creditdecisionengine.entity.CreditDecision;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link CreditDecision} audit records.
 */
public interface CreditDecisionRepository extends JpaRepository<CreditDecision, UUID> {

    /**
     * Looks up a previously recorded decision by its upstream idempotency request identifier.
     *
     * @param requestId unique request identifier from the Loan Origination System
     * @return existing credit decision if already processed
     */
    Optional<CreditDecision> findByRequestId(UUID requestId);
}
