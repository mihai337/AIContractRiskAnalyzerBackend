package licenta.mihai.aicontractriskanalyzerbackend.api.dto;

import licenta.mihai.aicontractriskanalyzerbackend.domain.model.ClauseType;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.RiskLevel;

public record MissingClauseDto(
    ClauseType type,
    String reason,
    RiskLevel severity
) {
}

