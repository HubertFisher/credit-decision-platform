package kz.alitulegenov.creditdecisionengine.domain.model;

/**
 * Standardized machine-readable reason codes explaining the outcome of a credit decision.
 *
 * <p>Used for audit trails, regulatory explainability, and downstream orchestration by the core backend.
 */
public enum DecisionReasonCode {
    /**
     * Borrower meets all policy criteria and can afford the requested principal on the requested term.
     */
    APPROVED_AS_REQUESTED,

    // --- Stage 1: Hard Stop / Knock-out Rules ---

    /**
     * Applicant is younger than the minimum eligible age at the time of application.
     */
    APPLICANT_TOO_YOUNG,

    /**
     * Applicant would exceed the maximum eligible age even on the shortest available product term.
     */
    AGE_AT_MATURITY_EXCEEDED,

    /**
     * Credit bureau report indicates an currently active overdue loan obligation.
     */
    ACTIVE_DELINQUENCY_PRESENT,

    /**
     * Historical delinquency in the last 12 months exceeds the maximum tolerated threshold.
     */
    SEVERE_HISTORICAL_DELINQUENCY,

    /**
     * External credit bureau score is below the minimum cut-off threshold.
     */
    CREDIT_SCORE_BELOW_MINIMUM,

    // --- Stage 3 & 5: Affordability and Minimum Threshold Rejections ---

    /**
     * Existing monthly debt obligations already consume or exceed the maximum allowed DTI ratio.
     */
    DTI_LIMIT_EXCEEDED,

    /**
     * Remaining income after existing debt obligations and the statutory subsistence minimum is non-positive.
     */
    INSUFFICIENT_DISPOSABLE_INCOME,

    /**
     * Maximum affordable principal across all eligible terms is below the product minimum amount.
     */
    CAPACITY_BELOW_MIN_PRODUCT_AMOUNT,

    // --- Stage 5: Counter-Offer Modification Reasons ---

    /**
     * Monthly installment for the requested amount on the requested term exceeds the available payment budget.
     */
    PAYMENT_CAPACITY_LIMITED_ON_REQUESTED_TERM,

    /**
     * Requested amount exceeds the maximum risk exposure cap assigned to the borrower's risk grade.
     */
    RISK_GRADE_CAP_APPLIED,

    /**
     * Requested amount exceeds the global maximum principal allowed for the credit product.
     */
    PRODUCT_MAX_AMOUNT_CAP_APPLIED,

    /**
     * Requested loan term was shortened because the borrower would exceed the maximum age at maturity.
     */
    TERM_LIMITED_BY_MATURITY_AGE
}
