package licenta.mihai.aicontractriskanalyzerbackend.domain.model;

import java.time.Instant;
import java.util.List;

public record ContractAnalysisResult(
    List<DetectedClause> detectedClauses,
    List<MissingClause> missingClauses,
    RiskScore riskScore,
    List<AiSuggestion> aiSuggestions,
    List<RuleAlert> ruleAlerts,
    Instant generatedAt,
    String contractType,
    Double contractTypeConfidence,
    Boolean isContract,
    String nonContractReason
) {
    public record DetectedClause(
        String id,
        ClauseType type,
        String title,
        String snippet,
        double confidence,
        RiskLevel riskLevel
    ) {
    }

    public record MissingClause(
        ClauseType type,
        String reason,
        RiskLevel severity
    ) {
    }

    public record AiSuggestion(
        String id,
        String title,
        String description,
        RiskLevel priority
    ) {
    }

    public record RuleAlert(
        String ruleId,
        String title,
        String description,
        RiskLevel severity
    ) {
    }

    public record RiskScore(
        int overallScore,
        RiskLevel riskLevel,
        List<String> rationale
    ) {
    }
}
