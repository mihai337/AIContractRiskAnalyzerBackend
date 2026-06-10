package licenta.mihai.aicontractriskanalyzerbackend.api.dto;

import licenta.mihai.aicontractriskanalyzerbackend.models.AnalysisJobStatus;

public record AnalysisJobDto(
    String id,
    String contractId,
    AnalysisJobStatus status,
    long createdAtEpochSeconds,
    Long startedAtEpochSeconds,
    Long completedAtEpochSeconds,
    String errorMessage
) {
}

