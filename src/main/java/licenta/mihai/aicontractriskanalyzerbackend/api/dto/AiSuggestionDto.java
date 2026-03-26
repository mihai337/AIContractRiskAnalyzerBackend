package licenta.mihai.aicontractriskanalyzerbackend.api.dto;

import licenta.mihai.aicontractriskanalyzerbackend.domain.model.RiskLevel;

public record AiSuggestionDto(
    String id,
    String title,
    String description,
    RiskLevel priority
) {
}

