package kz.alitulegenov.creditdecisionengine.domain.policy;

/**
 * Internal credit risk grade assigned to an applicant based on their external credit bureau score.
 *
 * <p>Determines both the individual nominal annual interest rate (Risk-Based Pricing)
 * and the maximum principal exposure cap per borrower.
 */
public enum RiskGrade {
    /**
     * Prime tier (score 750–850): lowest credit risk, preferential interest rate, full product exposure cap.
     */
    GRADE_A,

    /**
     * Near-prime tier (score 680–749): low credit risk, standard interest rate, high exposure cap.
     */
    GRADE_B,

    /**
     * Standard tier (score 600–679): moderate credit risk, elevated interest rate, medium exposure cap.
     */
    GRADE_C,

    /**
     * Subprime tier (score 520–599): high credit risk, maximum interest rate, strict exposure cap.
     */
    GRADE_D,

    /**
     * Unacceptable credit risk (score below minimum threshold): ineligible for lending.
     */
    REJECT
}
