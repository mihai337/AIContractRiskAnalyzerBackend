package licenta.mihai.aicontractriskanalyzerbackend.api.dto;

public record RetrievalEvidenceDto(
    String clauseId,
    String contractId,
    String clauseType,
    String snippet,
    double distance
) {
}

