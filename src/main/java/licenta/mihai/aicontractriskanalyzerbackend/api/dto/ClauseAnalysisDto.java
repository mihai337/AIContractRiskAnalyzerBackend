package licenta.mihai.aicontractriskanalyzerbackend.api.dto;

import licenta.mihai.aicontractriskanalyzerbackend.domain.model.ClauseType;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.RiskLevel;

public record ClauseAnalysisDto(
    String id,
    ClauseType type,
    String title,
    String snippet,
    double confidence,
    RiskLevel riskLevel
) {
}

