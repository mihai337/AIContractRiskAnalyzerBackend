package licenta.mihai.aicontractriskanalyzerbackend.api.dto;

import licenta.mihai.aicontractriskanalyzerbackend.domain.model.AnalysisStatus;

public record ContractRecordDto(
    String id,
    String fileName,
    String sourceUri,
    long uploadedAtEpochSeconds,
    AnalysisStatus status,
    ContractAnalysisDto analysis
) {
}

