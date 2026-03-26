package licenta.mihai.aicontractriskanalyzerbackend.application.engine;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.ClauseType;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.ContractAnalysisResult;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.RiskLevel;
import org.springframework.stereotype.Component;

@Component
public class MissingClauseDetectionService {

    public List<ContractAnalysisResult.MissingClause> detect(List<ContractAnalysisResult.DetectedClause> detectedClauses) {
        Set<ClauseType> detectedTypes = detectedClauses.stream().map(ContractAnalysisResult.DetectedClause::type).collect(java.util.stream.Collectors.toSet());

        return Arrays.stream(ClauseType.values())
            .filter(type -> type != ClauseType.OTHER)
            .filter(type -> !detectedTypes.contains(type))
            .map(type -> new ContractAnalysisResult.MissingClause(
                type,
                "Clause was not detected by the current rules/heuristics.",
                severityForMissing(type)
            ))
            .toList();
    }

    private RiskLevel severityForMissing(ClauseType type) {
        return switch (type) {
            case LIABILITY, DATA_PROTECTION, CONFIDENTIALITY -> RiskLevel.HIGH;
            case TERMINATION, PAYMENT, GOVERNING_LAW -> RiskLevel.MEDIUM;
            default -> RiskLevel.LOW;
        };
    }
}

