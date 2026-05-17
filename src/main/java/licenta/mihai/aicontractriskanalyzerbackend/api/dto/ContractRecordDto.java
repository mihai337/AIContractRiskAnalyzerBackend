package licenta.mihai.aicontractriskanalyzerbackend.api.dto;

import java.util.List;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.AnalysisStatus;

public record ContractRecordDto(
    String id,
    String fileName,
    String sourceUri,
    long uploadedAtEpochSeconds,
    AnalysisStatus status,
    ContractAnalysisDto analysis,
    List<String> selectedRuleIds
) {
}
