package licenta.mihai.aicontractriskanalyzerbackend.services;

import java.util.List;
import java.util.Set;
import licenta.mihai.aicontractriskanalyzerbackend.models.ClauseType;
import licenta.mihai.aicontractriskanalyzerbackend.models.ContractAnalysisResult;
import licenta.mihai.aicontractriskanalyzerbackend.models.RiskLevel;
import org.springframework.stereotype.Component;

@Component
public class MissingClauseDetectionService {

    /**
     * A clause type is "missing" when the user selected a rule requiring it (so it's in
     * {@code requiredTypes}) but it was not detected in the contract. This is the single
     * source of truth for missing clauses — it mirrors exactly what the user chose to check,
     * so a selected clause type ends up either in the detected clauses or in this list.
     */
    public List<ContractAnalysisResult.MissingClause> detect(
        Set<ClauseType> requiredTypes,
        Set<ClauseType> detectedTypes
    ) {
        if (requiredTypes == null || requiredTypes.isEmpty()) {
            return List.of();
        }
        return requiredTypes.stream()
            .filter(type -> !detectedTypes.contains(type))
            .map(type -> new ContractAnalysisResult.MissingClause(
                type,
                "No " + readable(type) + " clause was detected in the contract.",
                severityForMissing(type)
            ))
            .toList();
    }

    private static String readable(ClauseType type) {
        return type.name().toLowerCase().replace('_', ' ');
    }

    private RiskLevel severityForMissing(ClauseType type) {
        return switch (type) {
            case LIABILITY, DATA_PROTECTION, CONFIDENTIALITY -> RiskLevel.HIGH;
            case TERMINATION, PAYMENT, GOVERNING_LAW -> RiskLevel.MEDIUM;
            default -> RiskLevel.LOW;
        };
    }
}
