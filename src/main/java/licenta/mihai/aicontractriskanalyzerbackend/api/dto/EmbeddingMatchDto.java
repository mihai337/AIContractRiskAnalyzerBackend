package licenta.mihai.aicontractriskanalyzerbackend.api.dto;

public record EmbeddingMatchDto(
    String clauseId,
    String contractId,
    String clauseType,
    String snippet,
    double distance
) {
}

