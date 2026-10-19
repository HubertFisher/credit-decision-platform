package kz.alitulegenov.creditdecisionengine.domain.pricing;

import java.math.BigDecimal;
import java.util.Objects;
import kz.alitulegenov.creditdecisionengine.domain.policy.CreditPolicy;
import kz.alitulegenov.creditdecisionengine.domain.policy.RiskGrade;
import org.springframework.stereotype.Component;

/**
 * Stage 2 of the Decision Pipeline: Risk-Based Pricing and Exposure Cap Resolver.
 *
 * <p>Maps the applicant's external credit bureau score to an internal {@link RiskGrade},
 * determining their individualized nominal annual interest rate (APR) and maximum allowed
 * principal exposure cap:
 * <ul>
 *   <li>{@code 750–850 -> GRADE_A}: 16.0% APR, 10,000,000 KZT cap</li>
 *   <li>{@code 680–749 -> GRADE_B}: 20.0% APR, 6,000,000 KZT cap</li>
 *   <li>{@code 600–679 -> GRADE_C}: 25.0% APR, 3,000,000 KZT cap</li>
 *   <li>{@code 520–599 -> GRADE_D}: 32.0% APR, 1,000,000 KZT cap</li>
 *   <li>{@code < 520   -> REJECT}: ineligible</li>
 * </ul>
 */
@Component
public class RiskGradeResolver {

    private static final int GRADE_A_MIN_SCORE = 750;
    private static final int GRADE_B_MIN_SCORE = 680;
    private static final int GRADE_C_MIN_SCORE = 600;

    private static final BigDecimal GRADE_A_RATE = new BigDecimal("0.1600");
    private static final BigDecimal GRADE_B_RATE = new BigDecimal("0.2000");
    private static final BigDecimal GRADE_C_RATE = new BigDecimal("0.2500");
    private static final BigDecimal GRADE_D_RATE = new BigDecimal("0.3200");

    private static final BigDecimal GRADE_A_CAP = new BigDecimal("10000000.00");
    private static final BigDecimal GRADE_B_CAP = new BigDecimal("6000000.00");
    private static final BigDecimal GRADE_C_CAP = new BigDecimal("3000000.00");
    private static final BigDecimal GRADE_D_CAP = new BigDecimal("1000000.00");

    /**
     * Assigned risk grade, annual interest rate, and maximum principal limit for the borrower.
     *
     * @param riskGrade         resolved internal risk tier
     * @param nominalAnnualRate assigned nominal annual interest rate as a decimal fraction
     * @param riskExposureCap   maximum principal exposure allowed for this risk tier (in KZT)
     */
    public record RiskPricingResult(
            RiskGrade riskGrade,
            BigDecimal nominalAnnualRate,
            BigDecimal riskExposureCap
    ) {
        public RiskPricingResult {
            Objects.requireNonNull(riskGrade, "riskGrade must not be null");
            Objects.requireNonNull(nominalAnnualRate, "nominalAnnualRate must not be null");
            Objects.requireNonNull(riskExposureCap, "riskExposureCap must not be null");
        }
    }

    /**
     * Resolves the risk grade, interest rate, and exposure cap for the given credit score.
     */
    public RiskPricingResult resolve(int creditScore, CreditPolicy policy) {
        Objects.requireNonNull(policy, "policy must not be null");

        if (creditScore < policy.minCreditScore()) {
            return new RiskPricingResult(RiskGrade.REJECT, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        if (creditScore >= GRADE_A_MIN_SCORE) {
            return new RiskPricingResult(RiskGrade.GRADE_A, GRADE_A_RATE, GRADE_A_CAP);
        }
        if (creditScore >= GRADE_B_MIN_SCORE) {
            return new RiskPricingResult(RiskGrade.GRADE_B, GRADE_B_RATE, GRADE_B_CAP);
        }
        if (creditScore >= GRADE_C_MIN_SCORE) {
            return new RiskPricingResult(RiskGrade.GRADE_C, GRADE_C_RATE, GRADE_C_CAP);
        }
        return new RiskPricingResult(RiskGrade.GRADE_D, GRADE_D_RATE, GRADE_D_CAP);
    }
}
