package licenta.mihai.aicontractriskanalyzerbackend.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AnalyzeContractRequestDto(
    @NotNull List<String> selectedRuleIds
) {
}

