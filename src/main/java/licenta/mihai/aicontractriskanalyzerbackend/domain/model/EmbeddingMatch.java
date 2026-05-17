package licenta.mihai.aicontractriskanalyzerbackend.domain.model;

public record EmbeddingMatch(
    String clauseId,
    String contractId,
    String clauseType,
    String snippet,
    double distance
) {
}

