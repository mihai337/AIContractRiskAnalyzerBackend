package licenta.mihai.aicontractriskanalyzerbackend.api.dto;

import licenta.mihai.aicontractriskanalyzerbackend.models.RiskLevel;

public record ClauseIssueDto(
    String issueType,
    RiskLevel severity,
    String explanation,
    String highlightedText
) {
}

