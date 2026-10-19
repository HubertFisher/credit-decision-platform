package kz.alitulegenov.creditdecisionengine.domain.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import kz.alitulegenov.creditdecisionengine.domain.calculator.AnnuityLoanCalculator.ScheduleSummary;
import kz.alitulegenov.creditdecisionengine.domain.policy.CreditPolicy;
import org.junit.jupiter.api.Test;

class AnnuityLoanCalculatorTest {

    private final AnnuityLoanCalculator calculator = new AnnuityLoanCalculator();
    private final CreditPolicy policy = CreditPolicy.defaultPolicy();

    @Test
    void shouldCalculateStandardAnnuityScheduleCorrectly() {
        // Principal: 3,000,000 KZT, Term: 12 months, Rate: 20% (0.2000)
        // Monthly rate r = 0.20 / 12 = 0.016666666...
        // Expected monthly payment: 277,903.52 KZT
        ScheduleSummary summary = calculator.calculateSchedule(
                new BigDecimal("3000000.00"),
                12,
                new BigDecimal("0.2000")
        );

        assertEquals(new BigDecimal("3000000.00"), summary.principalAmount());
        assertEquals(12, summary.termMonths());
        assertEquals(new BigDecimal("0.2000"), summary.nominalAnnualRate());
        assertEquals(new BigDecimal("277903.52"), summary.monthlyPayment());
        assertEquals(new BigDecimal("3334842.24"), summary.totalPayableAmount());
        assertEquals(new BigDecimal("334842.24"), summary.totalInterestAmount());
    }

    @Test
    void shouldHandleZeroInterestPromotionalRate() {
        ScheduleSummary summary = calculator.calculateSchedule(
                new BigDecimal("1200000.00"),
                12,
                BigDecimal.ZERO
        );

        assertEquals(new BigDecimal("100000.00"), summary.monthlyPayment());
        assertEquals(new BigDecimal("1200000.00"), summary.totalPayableAmount());
        assertEquals(new BigDecimal("0.00"), summary.totalInterestAmount());
    }

    @Test
    void shouldCalculateMaxAffordablePrincipalSteppedDownTo10k() {
        // Budget: 145,000 KZT, Term: 12 months, Rate: 20% (0.2000), Cap: 6,000,000 KZT
        // Raw max principal = 145,000 / K ≈ 1,565,291.43 KZT
        // Stepped down to 10,000 KZT -> 1,560,000.00 KZT
        BigDecimal maxPrincipal = calculator.calculateMaxAffordablePrincipal(
                new BigDecimal("145000.00"),
                12,
                new BigDecimal("0.2000"),
                new BigDecimal("6000000.00"),
                policy
        );

        assertEquals(new BigDecimal("1560000.00"), maxPrincipal);

        ScheduleSummary verified = calculator.calculateSchedule(
                maxPrincipal,
                12,
                new BigDecimal("0.2000")
        );
        assertTrue(verified.monthlyPayment().compareTo(new BigDecimal("145000.00")) <= 0);
    }

    @Test
    void shouldRespectRiskExposureCapWhenBudgetIsVeryHigh() {
        // High budget: 500,000 KZT, but Grade D cap is 1,000,000 KZT
        BigDecimal maxPrincipal = calculator.calculateMaxAffordablePrincipal(
                new BigDecimal("500000.00"),
                24,
                new BigDecimal("0.3200"),
                new BigDecimal("1000000.00"),
                policy
        );

        assertEquals(new BigDecimal("1000000.00"), maxPrincipal);
    }

    @Test
    void shouldReturnZeroWhenBudgetIsZeroOrNegative() {
        BigDecimal maxPrincipal = calculator.calculateMaxAffordablePrincipal(
                BigDecimal.ZERO,
                12,
                new BigDecimal("0.2000"),
                new BigDecimal("6000000.00"),
                policy
        );

        assertEquals(new BigDecimal("0.00"), maxPrincipal);
    }

    @Test
    void shouldThrowExceptionOnInvalidInputs() {
        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calculateSchedule(BigDecimal.ZERO, 12, new BigDecimal("0.2000"))
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calculateSchedule(new BigDecimal("100000"), 0, new BigDecimal("0.2000"))
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calculateSchedule(new BigDecimal("100000"), 12, new BigDecimal("-0.01"))
        );
    }
}
