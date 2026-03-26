package licenta.mihai.aicontractriskanalyzerbackend.application.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.ClauseType;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.ContractAnalysisResult;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.RiskLevel;
import org.springframework.stereotype.Component;

@Component
public class ClauseDetectionService {

    private static final Map<ClauseType, List<String>> CLAUSE_KEYWORDS = Map.of(
        ClauseType.CONFIDENTIALITY, List.of("confidential", "non-disclosure", "nda"),
        ClauseType.TERMINATION, List.of("termination", "terminate", "notice period"),
        ClauseType.LIABILITY, List.of("liability", "indemnify", "damages"),
        ClauseType.PAYMENT, List.of("payment", "invoice", "fee"),
        ClauseType.DATA_PROTECTION, List.of("data protection", "gdpr", "personal data"),
        ClauseType.FORCE_MAJEURE, List.of("force majeure", "act of god"),
        ClauseType.GOVERNING_LAW, List.of("governing law", "jurisdiction"),
        ClauseType.INTELLECTUAL_PROPERTY, List.of("intellectual property", "ip", "ownership"),
        ClauseType.DISPUTE_RESOLUTION, List.of("arbitration", "dispute resolution", "mediation")
    );

    public List<ContractAnalysisResult.DetectedClause> detect(String text) {
        String normalized = text == null ? "" : text.toLowerCase();
        List<ContractAnalysisResult.DetectedClause> output = new ArrayList<>();

        for (Map.Entry<ClauseType, List<String>> entry : CLAUSE_KEYWORDS.entrySet()) {
            String hit = entry.getValue().stream().filter(normalized::contains).findFirst().orElse(null);
            if (hit != null) {
                output.add(new ContractAnalysisResult.DetectedClause(
                    UUID.randomUUID().toString(),
                    entry.getKey(),
                    entry.getKey().name().replace('_', ' '),
                    "Detected keyword: " + hit,
                    0.68,
                    defaultRiskForClause(entry.getKey())
                ));
            }
        }
        return output;
    }

    private RiskLevel defaultRiskForClause(ClauseType type) {
        return switch (type) {
            case LIABILITY, DATA_PROTECTION -> RiskLevel.HIGH;
            case TERMINATION, PAYMENT, DISPUTE_RESOLUTION -> RiskLevel.MEDIUM;
            default -> RiskLevel.LOW;
        };
    }
}

