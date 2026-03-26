package licenta.mihai.aicontractriskanalyzerbackend.api.dto;

import licenta.mihai.aicontractriskanalyzerbackend.domain.model.RiskLevel;

public record RuleAlertDto(
    String ruleId,
    String title,
    String description,
    RiskLevel severity
) {
}

