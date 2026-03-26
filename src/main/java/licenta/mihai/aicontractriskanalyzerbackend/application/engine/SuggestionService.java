package licenta.mihai.aicontractriskanalyzerbackend.application.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.ContractAnalysisResult;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.RiskLevel;
import org.springframework.stereotype.Component;

@Component
public class SuggestionService {

    public List<ContractAnalysisResult.AiSuggestion> suggest(
        List<ContractAnalysisResult.MissingClause> missingClauses,
        List<ContractAnalysisResult.RuleAlert> alerts
    ) {
        List<ContractAnalysisResult.AiSuggestion> suggestions = new ArrayList<>();

        for (ContractAnalysisResult.MissingClause missingClause : missingClauses.stream().limit(5).toList()) {
            suggestions.add(new ContractAnalysisResult.AiSuggestion(
                UUID.randomUUID().toString(),
                "Add " + missingClause.type() + " clause",
                "Consider adding a well-defined " + missingClause.type().name().toLowerCase().replace('_', ' ') + " clause.",
                missingClause.severity()
            ));
        }

        for (ContractAnalysisResult.RuleAlert alert : alerts.stream().limit(5).toList()) {
            suggestions.add(new ContractAnalysisResult.AiSuggestion(
                UUID.randomUUID().toString(),
                "Resolve rule alert: " + alert.ruleId(),
                alert.description(),
                alert.severity()
            ));
        }

        if (suggestions.isEmpty()) {
            suggestions.add(new ContractAnalysisResult.AiSuggestion(
                UUID.randomUUID().toString(),
                "No major issues detected",
                "Run a legal review for final validation before signing.",
                RiskLevel.LOW
            ));
        }

        return suggestions;
    }
}

