package licenta.mihai.aicontractriskanalyzerbackend.api.dto;

import java.util.List;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.RiskLevel;

public record RiskScoreDto(
    int overallScore,
    RiskLevel riskLevel,
    List<String> rationale
) {
}

