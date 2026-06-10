package licenta.mihai.aicontractriskanalyzerbackend.api.dto;

import licenta.mihai.aicontractriskanalyzerbackend.models.RiskLevel;

public record AiSuggestionDto(
    String id,
    String title,
    String description,
    RiskLevel priority
) {
}

