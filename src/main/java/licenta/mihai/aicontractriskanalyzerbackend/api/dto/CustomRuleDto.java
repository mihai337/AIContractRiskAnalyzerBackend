package licenta.mihai.aicontractriskanalyzerbackend.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.ClauseType;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.RiskLevel;

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

