package licenta.mihai.aicontractriskanalyzerbackend.models;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RiskLevel {
    LOW(20),
    MEDIUM(45),
    HIGH(70),
    CRITICAL(90);

    private final int score;

    public static RiskLevel fromScore(int rawScore) {
        if (rawScore >= 85) {
            return CRITICAL;
        }
        if (rawScore >= 60) {
            return HIGH;
        }
        if (rawScore >= 40) {
            return MEDIUM;
        }
        return LOW;
    }

    public static int fromLevelToScore(RiskLevel level) {
        return switch (level) {
            case LOW -> 20;
            case MEDIUM -> 45;
            case HIGH -> 70;
            case CRITICAL -> 90;
        };
    }

    public static int severityPenalty(RiskLevel level) {
        return switch (level) {
            case LOW -> 2;
            case MEDIUM -> 5;
            case HIGH -> 8;
            case CRITICAL -> 12;
        };
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }

}

