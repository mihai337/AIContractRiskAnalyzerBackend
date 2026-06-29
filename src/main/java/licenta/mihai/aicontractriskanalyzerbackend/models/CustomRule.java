package licenta.mihai.aicontractriskanalyzerbackend.models;

public record CustomRule(
    String id,
    String name,
    String description,
    ClauseType requiredClause,
    RiskLevel severity,
    boolean enabled
) {
}

