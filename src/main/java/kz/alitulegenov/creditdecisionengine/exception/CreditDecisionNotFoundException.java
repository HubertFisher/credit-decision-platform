package kz.alitulegenov.creditdecisionengine.exception;

import java.util.UUID;

/**
 * Thrown when a requested credit decision record does not exist in persistence.
 */
public class CreditDecisionNotFoundException extends RuntimeException {

    public CreditDecisionNotFoundException(UUID id) {
        super("Could not find credit decision with id: " + id);
    }
}
