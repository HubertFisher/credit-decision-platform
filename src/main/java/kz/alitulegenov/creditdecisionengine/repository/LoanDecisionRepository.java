package kz.alitulegenov.creditdecisionengine.repository;

import kz.alitulegenov.creditdecisionengine.entity.LoanDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface LoanDecisionRepository extends JpaRepository<LoanDecision, UUID> {}
