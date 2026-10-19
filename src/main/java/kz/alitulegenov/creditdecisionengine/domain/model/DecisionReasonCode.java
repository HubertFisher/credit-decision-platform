package kz.alitulegenov.creditdecisionengine.domain.model;

public enum DecisionReasonCode {
    APPROVED_AS_REQUESTED,

    // Hard stop rules (Stage 1)
    APPLICANT_TOO_YOUNG,
    AGE_AT_MATURITY_EXCEEDED,
    ACTIVE_DELINQUENCY_PRESENT,
    SEVERE_HISTORICAL_DELINQUENCY,
    CREDIT_SCORE_BELOW_MINIMUM,

    // Failures on solvency and minimum threshold (Stage 3 & 5)
    DTI_LIMIT_EXCEEDED,
    INSUFFICIENT_DISPOSABLE_INCOME,
    CAPACITY_BELOW_MIN_PRODUCT_AMOUNT,

    // Reasons for modifying conditions when COUNTER_OFFER (Stage 5)
    PAYMENT_CAPACITY_LIMITED_ON_REQUESTED_TERM,
    RISK_GRADE_CAP_APPLIED,
    PRODUCT_MAX_AMOUNT_CAP_APPLIED,
    TERM_LIMITED_BY_MATURITY_AGE
}
