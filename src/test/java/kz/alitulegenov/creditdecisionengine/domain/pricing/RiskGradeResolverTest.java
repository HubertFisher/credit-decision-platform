package kz.alitulegenov.creditdecisionengine.domain.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import kz.alitulegenov.creditdecisionengine.domain.policy.CreditPolicy;
import kz.alitulegenov.creditdecisionengine.domain.policy.RiskGrade;
import kz.alitulegenov.creditdecisionengine.domain.pricing.RiskGradeResolver.RiskPricingResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RiskGradeResolverTest {

    private final RiskGradeResolver resolver = new RiskGradeResolver();
    private final CreditPolicy policy = CreditPolicy.defaultPolicy();

    @ParameterizedTest
    @CsvSource({
            "850, GRADE_A, 0.1600, 10000000.00",
            "750, GRADE_A, 0.1600, 10000000.00",
            "749, GRADE_B, 0.2000, 6000000.00",
            "680, GRADE_B, 0.2000, 6000000.00",
            "679, GRADE_C, 0.2500, 3000000.00",
            "600, GRADE_C, 0.2500, 3000000.00",
            "599, GRADE_D, 0.3200, 1000000.00",
            "520, GRADE_D, 0.3200, 1000000.00"
    })
    void shouldResolveGradeRateAndCapForValidScores(
            int score,
            RiskGrade expectedGrade,
            String expectedRate,
            String expectedCap
    ) {
        RiskPricingResult result = resolver.resolve(score, policy);

        assertEquals(expectedGrade, result.riskGrade());
        assertEquals(new BigDecimal(expectedRate), result.nominalAnnualRate());
        assertEquals(new BigDecimal(expectedCap), result.riskExposureCap());
    }

    @Test
    void shouldReturnRejectWhenScoreBelowMinimum() {
        RiskPricingResult result = resolver.resolve(519, policy);

        assertEquals(RiskGrade.REJECT, result.riskGrade());
        assertEquals(BigDecimal.ZERO, result.nominalAnnualRate());
        assertEquals(BigDecimal.ZERO, result.riskExposureCap());
    }
}
