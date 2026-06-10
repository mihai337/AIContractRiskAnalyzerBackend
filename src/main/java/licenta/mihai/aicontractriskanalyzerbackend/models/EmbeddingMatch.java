package licenta.mihai.aicontractriskanalyzerbackend.models;

public record EmbeddingMatch(
    String clauseId,
    String contractId,
    String clauseType,
    String snippet,
    double distance
) {
}

