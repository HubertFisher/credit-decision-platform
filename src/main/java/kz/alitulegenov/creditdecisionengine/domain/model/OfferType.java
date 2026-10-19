package kz.alitulegenov.creditdecisionengine.domain.model;

/**
 * Classification of a generated loan offer relative to the borrower's original request.
 */
public enum OfferType {
    /**
     * Exact requested amount and requested term were approved without modification.
     */
    REQUESTED_TERMS,

    /**
     * Counter-offer Option A: preserves the requested term, but reduces the principal amount
     * to fit the borrower's monthly payment budget or risk/product caps.
     */
    REDUCED_AMOUNT_SAME_TERM,

    /**
     * Counter-offer Option B: extends the loan term using the policy's candidate term grid
     * so the borrower can qualify for the full requested amount (or a higher principal).
     */
    EXTENDED_TERM_FOR_AMOUNT
}
