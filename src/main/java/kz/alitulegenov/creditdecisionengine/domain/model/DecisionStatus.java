package kz.alitulegenov.creditdecisionengine.domain.model;

/**
 * Final outcome of the credit decision evaluation pipeline.
 */
public enum DecisionStatus {
    /**
     * The borrower qualifies for the exact requested amount and term.
     */
    APPROVED,

    /**
     * The borrower does not qualify for the exact requested terms, but qualifies for one or more
     * alternative offers (reduced principal on the same term or extended term for higher principal).
     */
    COUNTER_OFFER,

    /**
     * The credit request cannot be approved due to hard-stop rules, insufficient disposable income,
     * or maximum affordable principal falling below the minimum product threshold.
     */
    REJECTED
}
