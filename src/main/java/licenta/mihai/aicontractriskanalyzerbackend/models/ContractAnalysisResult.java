package licenta.mihai.aicontractriskanalyzerbackend.models;

import java.time.Instant;
import java.util.List;

public record ContractAnalysisResult(
    List<DetectedClause> detectedClauses,
    List<MissingClause> missingClauses,
    RiskScore riskScore,
    List<AiSuggestion> aiSuggestions,
    List<RuleAlert> ruleAlerts,
    List<ClauseInsight> clauseInsights,
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

    public record ClauseInsight(
        String clauseId,
        RiskLevel riskLevel,
        int riskScore,
        double confidence,
        String summary,
        String recommendation,
        List<Issue> issues
    ) {
    }

    public record Issue(
        String issueType,
        RiskLevel severity,
        String explanation,
        String highlightedText
    ) {
    }
}
