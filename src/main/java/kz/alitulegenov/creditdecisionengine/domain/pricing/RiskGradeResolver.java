package kz.alitulegenov.creditdecisionengine.domain.pricing;

import java.math.BigDecimal;
import kz.alitulegenov.creditdecisionengine.domain.policy.CreditPolicy;
import kz.alitulegenov.creditdecisionengine.domain.policy.RiskGrade;
import org.springframework.stereotype.Component;

@Component
public class RiskGradeResolver {

    public record RiskPricingResult(
        RiskGrade riskGrade,
        BigDecimal nominalAnnualRate,
        BigDecimal riskExposureCap
    ) {
    }

    public RiskPricingResult resolve(int creditScore, CreditPolicy policy) {
        if (creditScore < policy.minCreditScore()) {
            return new RiskPricingResult(RiskGrade.REJECT, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        if (creditScore >= 750) {
            return new RiskPricingResult(
                RiskGrade.GRADE_A,
                new BigDecimal("0.1600"),
                new BigDecimal("10000000.00")
            );
        }
        if (creditScore >= 680) {
            return new RiskPricingResult(
                RiskGrade.GRADE_B,
                new BigDecimal("0.2000"),
                new BigDecimal("6000000.00")
            );
        }
        if (creditScore >= 600) {
            return new RiskPricingResult(
                RiskGrade.GRADE_C,
                new BigDecimal("0.2500"),
                new BigDecimal("3000000.00")
            );
        }
        return new RiskPricingResult(
            RiskGrade.GRADE_D,
            new BigDecimal("0.3200"),
            new BigDecimal("1000000.00")
        );
    }
}
