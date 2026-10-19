package kz.alitulegenov.creditdecisionengine.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Represents a concrete commercial loan offer with full annuity repayment metrics
 * and projected post-loan debt-to-income (DTI) ratio.
 *
 * @param offerType           classification of the offer (requested, reduced amount, or extended term)
 * @param principalAmount     offered loan principal in KZT
 * @param termMonths          offered loan duration in months
 * @param nominalAnnualRate   assigned annual interest rate based on borrower's risk grade
 * @param monthlyPayment      fixed monthly annuity installment
 * @param totalInterestAmount total interest payable over the loan term
 * @param totalPayableAmount  total amount payable (principal + interest)
 * @param projectedDtiRatio   projected total DTI ratio including this new loan's installment
 */
public record LoanOffer(
    OfferType offerType,
    BigDecimal principalAmount,
    int termMonths,
    BigDecimal nominalAnnualRate,
    BigDecimal monthlyPayment,
    BigDecimal totalInterestAmount,
    BigDecimal totalPayableAmount,
    BigDecimal projectedDtiRatio
) {
    public LoanOffer {
        Objects.requireNonNull(offerType, "offerType must not be null");
        Objects.requireNonNull(principalAmount, "principalAmount must not be null");
        Objects.requireNonNull(nominalAnnualRate, "nominalAnnualRate must not be null");
        Objects.requireNonNull(monthlyPayment, "monthlyPayment must not be null");
        Objects.requireNonNull(totalInterestAmount, "totalInterestAmount must not be null");
        Objects.requireNonNull(totalPayableAmount, "totalPayableAmount must not be null");
        Objects.requireNonNull(projectedDtiRatio, "projectedDtiRatio must not be null");
    }
}
