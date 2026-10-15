package kz.alitulegenov.creditdecisionengine.risk;

import kz.alitulegenov.creditdecisionengine.dto.LoanDecisionRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class LoanRiskCalculator {

    public int calculateRisk(LoanDecisionRequest request) {
        int riskScore = 0;

        if (request.requestedAmount()
                .compareTo(request.income().multiply(BigDecimal.TEN)) > 0) {
            riskScore += 40;
        }

        if (request.termMonths() > 36) {
            riskScore += 20;
        }

        if (request.income().compareTo(new BigDecimal(300000)) < 0) {
            riskScore += 30;
        }

        return riskScore;
    }
}
