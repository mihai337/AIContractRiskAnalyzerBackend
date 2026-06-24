package licenta.mihai.aicontractriskanalyzerbackend.api.dto;

import java.util.List;
import licenta.mihai.aicontractriskanalyzerbackend.models.RiskLevel;

public record ClauseInsightDto(
    String clauseId,
    RiskLevel riskLevel,
    int riskScore,
    double confidence,
    String summary,
    String recommendation,
    List<ClauseIssueDto> issues
) {
}

