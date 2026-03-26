package licenta.mihai.aicontractriskanalyzerbackend.domain.model;

public record CustomRule(
    String id,
    String name,
    String description,
    ClauseType requiredClause,
    String keyword,
    RiskLevel severity,
    boolean enabled
) {
}

