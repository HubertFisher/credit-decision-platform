package kz.alitulegenov.creditdecisionengine.domain.offer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import kz.alitulegenov.creditdecisionengine.domain.affordability.AffordabilityCalculator;
import kz.alitulegenov.creditdecisionengine.domain.affordability.AffordabilityCalculator.AffordabilityResult;
import kz.alitulegenov.creditdecisionengine.domain.calculator.AnnuityLoanCalculator;
import kz.alitulegenov.creditdecisionengine.domain.calculator.AnnuityLoanCalculator.ScheduleSummary;
import kz.alitulegenov.creditdecisionengine.domain.model.DecisionInput;
import kz.alitulegenov.creditdecisionengine.domain.model.DecisionReasonCode;
import kz.alitulegenov.creditdecisionengine.domain.model.DecisionResult;
import kz.alitulegenov.creditdecisionengine.domain.model.DecisionStatus;
import kz.alitulegenov.creditdecisionengine.domain.model.LoanOffer;
import kz.alitulegenov.creditdecisionengine.domain.model.OfferType;
import kz.alitulegenov.creditdecisionengine.domain.policy.CreditPolicy;
import kz.alitulegenov.creditdecisionengine.domain.pricing.RiskGradeResolver;
import kz.alitulegenov.creditdecisionengine.domain.pricing.RiskGradeResolver.RiskPricingResult;
import kz.alitulegenov.creditdecisionengine.domain.rule.HardStopEvaluator;
import org.springframework.stereotype.Component;

/**
 * Orchestrates the 5-stage commercial Credit Decision Pipeline:
 * <ol>
 *   <li>Stage 1: Hard-stop / knock-out eligibility screening ({@link HardStopEvaluator})</li>
 *   <li>Stage 2: Risk-grade assignment and risk-based pricing ({@link RiskGradeResolver})</li>
 *   <li>Stage 3: Dual-constraint solvency and payment budget calculation ({@link AffordabilityCalculator})</li>
 *   <li>Stage 4: Annuity repayment schedule and reverse capacity math ({@link AnnuityLoanCalculator})</li>
 *   <li>Stage 5: Verdict determination and smart counter-offer matrix generation</li>
 * </ol>
 */
@Component
public class DecisionPipeline {

    private static final int MONTHS_IN_YEAR = 12;
    private static final int RATIO_SCALE = 4;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

    private final HardStopEvaluator hardStopEvaluator;
    private final RiskGradeResolver riskGradeResolver;
    private final AffordabilityCalculator affordabilityCalculator;
    private final AnnuityLoanCalculator annuityLoanCalculator;

    public DecisionPipeline(
        HardStopEvaluator hardStopEvaluator,
        RiskGradeResolver riskGradeResolver,
        AffordabilityCalculator affordabilityCalculator,
        AnnuityLoanCalculator annuityLoanCalculator
    ) {
        this.hardStopEvaluator = hardStopEvaluator;
        this.riskGradeResolver = riskGradeResolver;
        this.affordabilityCalculator = affordabilityCalculator;
        this.annuityLoanCalculator = annuityLoanCalculator;
    }

    /**
     * Executes the full decision pipeline using the default commercial credit policy.
     */
    public DecisionResult evaluate(DecisionInput input) {
        return evaluate(input, CreditPolicy.defaultPolicy());
    }

    /**
     * Executes the full decision pipeline against the specified credit policy.
     */
    public DecisionResult evaluate(DecisionInput input, CreditPolicy policy) {
        Objects.requireNonNull(input, "input must not be null");
        Objects.requireNonNull(policy, "policy must not be null");

        // Stage 1: Hard-stop screening
        List<DecisionReasonCode> hardStopReasons = hardStopEvaluator.evaluate(input, policy);

        // Stage 2: Risk-based pricing
        RiskPricingResult pricing = riskGradeResolver.resolve(input.creditScore(), policy);

        // Stage 3: Affordability & disposable income assessment
        AffordabilityResult affordability = affordabilityCalculator.evaluate(input, policy);

        if (!hardStopReasons.isEmpty()) {
            return new DecisionResult(
                DecisionStatus.REJECTED,
                pricing.riskGrade(),
                pricing.nominalAnnualRate(),
                pricing.riskExposureCap(),
                affordability.currentDtiRatio(),
                affordability.availableMonthlyBudget(),
                hardStopReasons,
                List.of()
            );
        }

        if (!affordability.isAffordable()) {
            return new DecisionResult(
                DecisionStatus.REJECTED,
                pricing.riskGrade(),
                pricing.nominalAnnualRate(),
                pricing.riskExposureCap(),
                affordability.currentDtiRatio(),
                affordability.availableMonthlyBudget(),
                affordability.rejectionReasons(),
                List.of()
            );
        }

        // Stage 4 & 5: Evaluate requested terms and generate counter-offers if needed
        int maxAllowedTermByAge = Math.min(
            policy.maxTermMonths(),
            (policy.maxAgeAtMaturity() - input.applicantAgeYears()) * MONTHS_IN_YEAR
        );

        BigDecimal effectiveMaxCap = pricing.riskExposureCap().min(policy.maxProductAmount());
        BigDecimal requestedAmount = input.requestedAmount();
        int requestedTerm = input.requestedTermMonths();

        List<DecisionReasonCode> modificationReasons = new ArrayList<>();
        if (requestedTerm > maxAllowedTermByAge) {
            modificationReasons.add(DecisionReasonCode.TERM_LIMITED_BY_MATURITY_AGE);
        }
        if (requestedAmount.compareTo(pricing.riskExposureCap()) > 0) {
            modificationReasons.add(DecisionReasonCode.RISK_GRADE_CAP_APPLIED);
        }
        if (requestedAmount.compareTo(policy.maxProductAmount()) > 0) {
            modificationReasons.add(DecisionReasonCode.PRODUCT_MAX_AMOUNT_CAP_APPLIED);
        }

        ScheduleSummary requestedSchedule = annuityLoanCalculator.calculateSchedule(
            requestedAmount,
            requestedTerm,
            pricing.nominalAnnualRate()
        );
        boolean paymentFitsBudget = requestedSchedule.monthlyPayment()
            .compareTo(affordability.availableMonthlyBudget()) <= 0;

        if (!paymentFitsBudget) {
            modificationReasons.add(DecisionReasonCode.PAYMENT_CAPACITY_LIMITED_ON_REQUESTED_TERM);
        }

        // Direct approval if no caps or age limits were breached and installment fits budget
        if (modificationReasons.isEmpty()) {
            LoanOffer approvedOffer = buildOffer(
                OfferType.REQUESTED_TERMS,
                requestedSchedule,
                input
            );
            return new DecisionResult(
                DecisionStatus.APPROVED,
                pricing.riskGrade(),
                pricing.nominalAnnualRate(),
                pricing.riskExposureCap(),
                affordability.currentDtiRatio(),
                affordability.availableMonthlyBudget(),
                List.of(DecisionReasonCode.APPROVED_AS_REQUESTED),
                List.of(approvedOffer)
            );
        }

        // Build counter-offer matrix (Option A: same/age-capped term, Option B: extended term)
        List<LoanOffer> counterOffers = buildCounterOffers(
            input,
            policy,
            pricing,
            affordability,
            maxAllowedTermByAge,
            effectiveMaxCap
        );

        if (counterOffers.isEmpty()) {
            return new DecisionResult(
                DecisionStatus.REJECTED,
                pricing.riskGrade(),
                pricing.nominalAnnualRate(),
                pricing.riskExposureCap(),
                affordability.currentDtiRatio(),
                affordability.availableMonthlyBudget(),
                List.of(DecisionReasonCode.CAPACITY_BELOW_MIN_PRODUCT_AMOUNT),
                List.of()
            );
        }

        return new DecisionResult(
            DecisionStatus.COUNTER_OFFER,
            pricing.riskGrade(),
            pricing.nominalAnnualRate(),
            pricing.riskExposureCap(),
            affordability.currentDtiRatio(),
            affordability.availableMonthlyBudget(),
            modificationReasons,
            counterOffers
        );
    }

    private List<LoanOffer> buildCounterOffers(
        DecisionInput input,
        CreditPolicy policy,
        RiskPricingResult pricing,
        AffordabilityResult affordability,
        int maxAllowedTermByAge,
        BigDecimal effectiveMaxCap
    ) {
        List<LoanOffer> offers = new ArrayList<>();
        int baseTerm = Math.min(input.requestedTermMonths(), maxAllowedTermByAge);

        // Option A: Maximum affordable principal on the base (requested or age-capped) term
        BigDecimal baseTermMaxPrincipal = annuityLoanCalculator.calculateMaxAffordablePrincipal(
            affordability.availableMonthlyBudget(),
            baseTerm,
            pricing.nominalAnnualRate(),
            pricing.riskExposureCap(),
            policy
        ).min(input.requestedAmount());

        if (baseTermMaxPrincipal.compareTo(policy.minProductAmount()) >= 0) {
            ScheduleSummary baseSchedule = annuityLoanCalculator.calculateSchedule(
                baseTermMaxPrincipal,
                baseTerm,
                pricing.nominalAnnualRate()
            );
            offers.add(buildOffer(OfferType.REDUCED_AMOUNT_SAME_TERM, baseSchedule, input));
        }

        // Option B: Try extending the term along candidateTerms to reach targetPrincipal
        BigDecimal targetPrincipal = input.requestedAmount().min(effectiveMaxCap);
        if (targetPrincipal.compareTo(baseTermMaxPrincipal) > 0
            && targetPrincipal.compareTo(policy.minProductAmount()) >= 0) {
            for (int candidateTerm : policy.candidateTerms()) {
                if (candidateTerm <= baseTerm || candidateTerm > maxAllowedTermByAge) {
                    continue;
                }
                ScheduleSummary extendedSchedule = annuityLoanCalculator.calculateSchedule(
                    targetPrincipal,
                    candidateTerm,
                    pricing.nominalAnnualRate()
                );
                if (extendedSchedule.monthlyPayment().compareTo(affordability.availableMonthlyBudget()) <= 0) {
                    offers.add(buildOffer(OfferType.EXTENDED_TERM_FOR_AMOUNT, extendedSchedule, input));
                    break;
                }
            }
        }

        return offers;
    }

    private LoanOffer buildOffer(
        OfferType offerType,
        ScheduleSummary schedule,
        DecisionInput input
    ) {
        BigDecimal totalMonthlyDebt = input.existingMonthlyPayments().add(schedule.monthlyPayment());
        BigDecimal projectedDtiRatio = totalMonthlyDebt.divide(
            input.monthlyIncome(),
            RATIO_SCALE,
            ROUNDING_MODE
        );

        return new LoanOffer(
            offerType,
            schedule.principalAmount(),
            schedule.termMonths(),
            schedule.nominalAnnualRate(),
            schedule.monthlyPayment(),
            schedule.totalInterestAmount(),
            schedule.totalPayableAmount(),
            projectedDtiRatio
        );
    }
}
