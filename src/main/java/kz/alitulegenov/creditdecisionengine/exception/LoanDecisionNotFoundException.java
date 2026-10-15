package kz.alitulegenov.creditdecisionengine.exception;

import java.util.UUID;

public class LoanDecisionNotFoundException extends RuntimeException {

    public LoanDecisionNotFoundException(UUID id) {
        super("Could not find loan decision with id: " + id);
    }
}
