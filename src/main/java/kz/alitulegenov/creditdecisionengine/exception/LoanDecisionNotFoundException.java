package kz.alitulegenov.creditdecisionengine.exception;

import java.util.UUID;

/**
 * Thrown when a requested credit decision record does not exist in persistence.
 */
public class LoanDecisionNotFoundException extends RuntimeException {

    public LoanDecisionNotFoundException(UUID id) {
        super("Could not find credit decision with id: " + id);
    }
}
