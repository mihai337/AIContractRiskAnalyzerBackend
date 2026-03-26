package licenta.mihai.aicontractriskanalyzerbackend.domain.model;

public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public static RiskLevel fromScore(int score) {
        if (score >= 80) {
            return LOW;
        }
        if (score >= 60) {
            return MEDIUM;
        }
        if (score >= 35) {
            return HIGH;
        }
        return CRITICAL;
    }
}

