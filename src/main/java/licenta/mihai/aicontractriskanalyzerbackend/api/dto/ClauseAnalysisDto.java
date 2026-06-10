package licenta.mihai.aicontractriskanalyzerbackend.api.dto;

import licenta.mihai.aicontractriskanalyzerbackend.models.ClauseType;
import licenta.mihai.aicontractriskanalyzerbackend.models.RiskLevel;

public record ClauseAnalysisDto(
    String id,
    ClauseType type,
    String title,
    String snippet,
    double confidence,
    RiskLevel riskLevel
) {
}

