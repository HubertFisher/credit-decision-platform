package kz.alitulegenov.creditdecisionengine.domain.calculator;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Objects;
import kz.alitulegenov.creditdecisionengine.domain.policy.CreditPolicy;
import org.springframework.stereotype.Component;

/**
 * Financial calculator for standard annuity consumer loans (equal monthly installments).
 *
 * <p>Mathematical model:
 * <ul>
 *   <li>Monthly interest rate: {@code r = nominalAnnualRate / 12}</li>
 *   <li>Annuity factor: {@code K = (r * (1 + r)^N) / ((1 + r)^N - 1)}</li>
 *   <li>Fixed monthly installment: {@code PMT = round(principal * K, 2, HALF_EVEN)}</li>
 * </ul>
 *
 * <p>All intermediate floating-point divisions and exponentiations use {@link MathContext#DECIMAL128}
 * (34 decimal digits of precision, IEEE 754R Decimal128) to prevent precision drift before
 * final monetary rounding to 2 decimal places using banker's rounding ({@link RoundingMode#HALF_EVEN}).
 */
@Component
public class AnnuityLoanCalculator {

    private static final BigDecimal MONTHS_IN_YEAR = BigDecimal.valueOf(12);
    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_EVEN;
    private static final MathContext CALCULATION_CONTEXT = MathContext.DECIMAL128;

    /**
     * Immutable summary of an annuity repayment schedule.
     *
     * @param principalAmount    approved or evaluated loan principal (S)
     * @param termMonths         loan duration in months (N)
     * @param nominalAnnualRate  annual nominal interest rate as a decimal fraction (e.g. 0.2000 for 20%)
     * @param monthlyPayment     fixed monthly annuity installment (PMT)
     * @param totalInterestAmount total interest over the full loan term
     * @param totalPayableAmount total amount payable by the borrower (principal + interest)
     */
    public record ScheduleSummary(
        BigDecimal principalAmount,
        int termMonths,
        BigDecimal nominalAnnualRate,
        BigDecimal monthlyPayment,
        BigDecimal totalInterestAmount,
        BigDecimal totalPayableAmount
    ) {
        public ScheduleSummary {
            Objects.requireNonNull(principalAmount, "principalAmount must not be null");
            Objects.requireNonNull(nominalAnnualRate, "nominalAnnualRate must not be null");
            Objects.requireNonNull(monthlyPayment, "monthlyPayment must not be null");
            Objects.requireNonNull(totalInterestAmount, "totalInterestAmount must not be null");
            Objects.requireNonNull(totalPayableAmount, "totalPayableAmount must not be null");
        }
    }

    /**
     * Calculates the annuity payment schedule metrics for a given principal, term, and rate.
     */
    public ScheduleSummary calculateSchedule(
        BigDecimal principalAmount,
        int termMonths,
        BigDecimal nominalAnnualRate
    ) {
        Objects.requireNonNull(principalAmount, "principalAmount must not be null");
        Objects.requireNonNull(nominalAnnualRate, "nominalAnnualRate must not be null");
        if (principalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("principalAmount must be positive");
        }
        if (termMonths <= 0) {
            throw new IllegalArgumentException("termMonths must be positive");
        }
        if (nominalAnnualRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("nominalAnnualRate must not be negative");
        }

        BigDecimal normalizedPrincipal = principalAmount.setScale(MONEY_SCALE, MONEY_ROUNDING);

        // Edge case: 0% promotional rate (avoids division by zero in annuity formula)
        if (nominalAnnualRate.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal zeroRatePayment = normalizedPrincipal
                .divide(BigDecimal.valueOf(termMonths), MONEY_SCALE, MONEY_ROUNDING);
            return new ScheduleSummary(
                normalizedPrincipal,
                termMonths,
                nominalAnnualRate,
                zeroRatePayment,
                BigDecimal.ZERO.setScale(MONEY_SCALE, MONEY_ROUNDING),
                normalizedPrincipal
            );
        }

        BigDecimal annuityFactor = calculateAnnuityFactor(termMonths, nominalAnnualRate);

        // Monthly installment: PMT = S * K, rounded to 2 decimal places (tiyns/cents)
        BigDecimal monthlyPayment = normalizedPrincipal
            .multiply(annuityFactor, CALCULATION_CONTEXT)
            .setScale(MONEY_SCALE, MONEY_ROUNDING);

        // Total amount paid across all N months: Total = PMT * N
        BigDecimal totalPayable = monthlyPayment
            .multiply(BigDecimal.valueOf(termMonths))
            .setScale(MONEY_SCALE, MONEY_ROUNDING);

        // Total interest cost: Interest = Total - Principal
        BigDecimal totalInterest = totalPayable
            .subtract(normalizedPrincipal)
            .max(BigDecimal.ZERO)
            .setScale(MONEY_SCALE, MONEY_ROUNDING);

        return new ScheduleSummary(
            normalizedPrincipal,
            termMonths,
            nominalAnnualRate,
            monthlyPayment,
            totalInterest,
            totalPayable
        );
    }

    /**
     * Performs reverse annuity calculation to find the maximum loan principal that fits within
     * the borrower's available monthly budget, capped by risk exposure and product limits,
     * and rounded down to the configured credit step (e.g. 10,000 KZT).
     */
    public BigDecimal calculateMaxAffordablePrincipal(
        BigDecimal availableMonthlyBudget,
        int termMonths,
        BigDecimal nominalAnnualRate,
        BigDecimal riskExposureCap,
        CreditPolicy policy
    ) {
        Objects.requireNonNull(availableMonthlyBudget, "availableMonthlyBudget must not be null");
        Objects.requireNonNull(nominalAnnualRate, "nominalAnnualRate must not be null");
        Objects.requireNonNull(riskExposureCap, "riskExposureCap must not be null");
        Objects.requireNonNull(policy, "policy must not be null");
        if (termMonths <= 0) {
            throw new IllegalArgumentException("termMonths must be positive");
        }

        if (availableMonthlyBudget.compareTo(BigDecimal.ZERO) <= 0
            || riskExposureCap.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, MONEY_ROUNDING);
        }

        // 1. Theoretical upper bound from inverse annuity formula: S_raw = Budget / K
        BigDecimal rawMaxPrincipal;
        if (nominalAnnualRate.compareTo(BigDecimal.ZERO) == 0) {
            rawMaxPrincipal = availableMonthlyBudget
                .multiply(BigDecimal.valueOf(termMonths))
                .setScale(MONEY_SCALE, RoundingMode.FLOOR);
        } else {
            BigDecimal annuityFactor = calculateAnnuityFactor(termMonths, nominalAnnualRate);
            rawMaxPrincipal = availableMonthlyBudget.divide(
                annuityFactor,
                MONEY_SCALE,
                RoundingMode.FLOOR
            );
        }

        // 2. Apply risk-grade exposure cap and product maximum limit
        BigDecimal cappedPrincipal = rawMaxPrincipal
            .min(riskExposureCap)
            .min(policy.maxProductAmount());

        // 3. Round strictly down (FLOOR) to the nearest credit amount step (e.g. 10,000 KZT)
        BigDecimal step = policy.amountStep();
        BigDecimal steppedPrincipal = cappedPrincipal
            .divide(step, 0, RoundingMode.FLOOR)
            .multiply(step)
            .setScale(MONEY_SCALE, MONEY_ROUNDING);

        // 4. Verification loop: ensure rounded schedule payment strictly fits budget
        while (steppedPrincipal.compareTo(BigDecimal.ZERO) > 0) {
            ScheduleSummary summary = calculateSchedule(steppedPrincipal, termMonths, nominalAnnualRate);
            if (summary.monthlyPayment().compareTo(availableMonthlyBudget) <= 0) {
                return steppedPrincipal;
            }
            steppedPrincipal = steppedPrincipal.subtract(step);
        }

        return BigDecimal.ZERO.setScale(MONEY_SCALE, MONEY_ROUNDING);
    }

    /**
     * Computes the exact annuity factor: {@code K = (r * (1 + r)^N) / ((1 + r)^N - 1)}.
     */
    private BigDecimal calculateAnnuityFactor(int termMonths, BigDecimal nominalAnnualRate) {
        BigDecimal monthlyRate = nominalAnnualRate.divide(MONTHS_IN_YEAR, CALCULATION_CONTEXT);
        BigDecimal compoundFactor = BigDecimal.ONE
            .add(monthlyRate, CALCULATION_CONTEXT)
            .pow(termMonths, CALCULATION_CONTEXT);

        BigDecimal numerator = monthlyRate.multiply(compoundFactor, CALCULATION_CONTEXT);
        BigDecimal denominator = compoundFactor.subtract(BigDecimal.ONE, CALCULATION_CONTEXT);

        return numerator.divide(denominator, CALCULATION_CONTEXT);
    }
}
