package licenta.mihai.aicontractriskanalyzerbackend.application.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.ClauseType;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.ContractAnalysisResult;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.CustomRule;
import org.springframework.stereotype.Component;

@Component
public class RuleEngineService {

    public List<ContractAnalysisResult.RuleAlert> evaluate(
        String extractedText,
        List<CustomRule> rules,
        List<ContractAnalysisResult.DetectedClause> detectedClauses
    ) {
        String normalized = extractedText == null ? "" : extractedText.toLowerCase();
        Set<ClauseType> detectedTypes = detectedClauses.stream().map(ContractAnalysisResult.DetectedClause::type)
            .collect(java.util.stream.Collectors.toSet());

        List<ContractAnalysisResult.RuleAlert> alerts = new ArrayList<>();
        for (CustomRule rule : rules) {
            if (!rule.enabled()) {
                continue;
            }

            if (rule.keyword() != null && !rule.keyword().isBlank() && !normalized.contains(rule.keyword().toLowerCase())) {
                alerts.add(new ContractAnalysisResult.RuleAlert(
                    rule.id(),
                    rule.name(),
                    "Required keyword is missing: " + rule.keyword(),
                    rule.severity()
                ));
            }

            if (rule.requiredClause() != null && !detectedTypes.contains(rule.requiredClause())) {
                alerts.add(new ContractAnalysisResult.RuleAlert(
                    rule.id(),
                    rule.name(),
                    "Required clause is missing: " + rule.requiredClause(),
                    rule.severity()
                ));
            }
        }

        return alerts;
    }
}

