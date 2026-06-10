package licenta.mihai.aicontractriskanalyzerbackend.api.dto;

import licenta.mihai.aicontractriskanalyzerbackend.models.RiskLevel;

public record RuleAlertDto(
    String ruleId,
    String title,
    String description,
    RiskLevel severity
) {
}

