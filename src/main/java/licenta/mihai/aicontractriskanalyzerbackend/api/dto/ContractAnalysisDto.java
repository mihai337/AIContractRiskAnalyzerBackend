package licenta.mihai.aicontractriskanalyzerbackend.api.dto;

import java.util.List;

public record ContractAnalysisDto(
    List<ClauseAnalysisDto> detectedClauses,
    List<MissingClauseDto> missingClauses,
    RiskScoreDto riskScore,
    List<ClauseInsightDto> clauseInsights,
    long generatedAtEpochSeconds,
    String contractType,
    Double contractTypeConfidence,
    Boolean isContract,
    String nonContractReason
) {
}
