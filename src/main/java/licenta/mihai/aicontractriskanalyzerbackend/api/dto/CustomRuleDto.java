package licenta.mihai.aicontractriskanalyzerbackend.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import licenta.mihai.aicontractriskanalyzerbackend.models.ClauseType;
import licenta.mihai.aicontractriskanalyzerbackend.models.RiskLevel;

public record CustomRuleDto(
    @NotBlank String id,
    @NotBlank String name,
    @NotBlank String description,
    ClauseType requiredClause,
    String keyword,
    @NotNull RiskLevel severity,
    boolean enabled
) {
}

