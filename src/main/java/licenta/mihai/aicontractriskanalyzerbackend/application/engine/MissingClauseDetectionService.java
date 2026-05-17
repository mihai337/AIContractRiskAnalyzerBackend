package licenta.mihai.aicontractriskanalyzerbackend.application.engine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.ClauseType;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.ContractAnalysisResult;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.RiskLevel;
import org.springframework.stereotype.Component;

@Component
public class MissingClauseDetectionService {

    private static final Map<String, List<ClauseType>> REQUIRED_BY_TYPE = buildRequiredByType();

    public List<ContractAnalysisResult.MissingClause> detect(
        List<ContractAnalysisResult.DetectedClause> detectedClauses,
        String contractType,
        Boolean isContract
    ) {
        if (Boolean.FALSE.equals(isContract)) {
            return List.of();
        }
        List<ClauseType> required = REQUIRED_BY_TYPE.getOrDefault(normalizeType(contractType), defaultRequired());
        Set<ClauseType> detectedTypes = detectedClauses.stream().map(ContractAnalysisResult.DetectedClause::type)
            .collect(java.util.stream.Collectors.toSet());

        return required.stream()
            .filter(type -> !detectedTypes.contains(type))
            .map(type -> new ContractAnalysisResult.MissingClause(
                type,
                "Clause was not detected for this contract type.",
                severityForMissing(type)
            ))
            .toList();
    }

    private static Map<String, List<ClauseType>> buildRequiredByType() {
        Map<String, List<ClauseType>> map = new HashMap<>();
        map.put("NDA", List.of(ClauseType.CONFIDENTIALITY, ClauseType.TERMINATION, ClauseType.LIABILITY, ClauseType.GOVERNING_LAW));
        map.put("SERVICE_AGREEMENT", List.of(ClauseType.PAYMENT, ClauseType.TERMINATION, ClauseType.LIABILITY, ClauseType.CONFIDENTIALITY, ClauseType.GOVERNING_LAW));
        map.put("EMPLOYMENT", List.of(ClauseType.PAYMENT, ClauseType.TERMINATION, ClauseType.CONFIDENTIALITY, ClauseType.GOVERNING_LAW));
        map.put("SALES", List.of(ClauseType.PAYMENT, ClauseType.LIABILITY, ClauseType.TERMINATION, ClauseType.GOVERNING_LAW));
        map.put("LEASE", List.of(ClauseType.PAYMENT, ClauseType.TERMINATION, ClauseType.LIABILITY, ClauseType.GOVERNING_LAW));
        map.put("ELECTRICITY_SUPPLY", List.of(ClauseType.PAYMENT, ClauseType.TERMINATION, ClauseType.LIABILITY, ClauseType.GOVERNING_LAW));
        return map;
    }

    private static String normalizeType(String contractType) {
        if (contractType == null) {
            return "";
        }
        return contractType.trim().toUpperCase();
    }

    private static List<ClauseType> defaultRequired() {
        return List.of(ClauseType.CONFIDENTIALITY, ClauseType.TERMINATION, ClauseType.LIABILITY, ClauseType.PAYMENT, ClauseType.GOVERNING_LAW);
    }

    private RiskLevel severityForMissing(ClauseType type) {
        return switch (type) {
            case LIABILITY, DATA_PROTECTION, CONFIDENTIALITY -> RiskLevel.HIGH;
            case TERMINATION, PAYMENT, GOVERNING_LAW -> RiskLevel.MEDIUM;
            default -> RiskLevel.LOW;
        };
    }
}
