package licenta.mihai.aicontractriskanalyzerbackend.application.engine;

import java.util.ArrayList;
import java.util.List;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.ContractAnalysisResult;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.RiskLevel;
import org.springframework.stereotype.Component;

@Component
public class RiskScoringService {

    public ContractAnalysisResult.RiskScore calculate(
        List<ContractAnalysisResult.DetectedClause> detectedClauses,
        List<ContractAnalysisResult.MissingClause> missingClauses,
        List<ContractAnalysisResult.RuleAlert> ruleAlerts
    ) {
        int score = 100;
        List<String> rationale = new ArrayList<>();

        score -= missingClauses.size() * 6;
        if (!missingClauses.isEmpty()) {
            rationale.add("Missing clauses detected: " + missingClauses.size());
        }

        int highSeverityAlerts = (int) ruleAlerts.stream().filter(a -> a.severity() == RiskLevel.HIGH || a.severity() == RiskLevel.CRITICAL).count();
        score -= highSeverityAlerts * 10;
        if (highSeverityAlerts > 0) {
            rationale.add("High severity rule alerts: " + highSeverityAlerts);
        }

        long highRiskClauses = detectedClauses.stream().filter(c -> c.riskLevel() == RiskLevel.HIGH || c.riskLevel() == RiskLevel.CRITICAL).count();
        score -= (int) highRiskClauses * 4;
        if (highRiskClauses > 0) {
            rationale.add("High-risk detected clauses: " + highRiskClauses);
        }

        score = Math.max(0, Math.min(100, score));
        rationale.add("Score computed from deterministic rule-based engine.");
        return new ContractAnalysisResult.RiskScore(score, RiskLevel.fromScore(score), rationale);
    }
}

