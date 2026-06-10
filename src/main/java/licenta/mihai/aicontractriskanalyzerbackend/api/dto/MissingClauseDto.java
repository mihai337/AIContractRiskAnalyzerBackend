package licenta.mihai.aicontractriskanalyzerbackend.api.dto;

import licenta.mihai.aicontractriskanalyzerbackend.models.ClauseType;
import licenta.mihai.aicontractriskanalyzerbackend.models.RiskLevel;

public record MissingClauseDto(
    ClauseType type,
    String reason,
    RiskLevel severity
) {
}

