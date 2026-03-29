package licenta.mihai.aicontractriskanalyzerbackend.application.port;

import java.util.List;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.ContractAnalysisResult;

public interface MlInferencePort {

    MlInferenceResult analyzeContract(String contractId, String extractedText);

    record MlInferenceResult(
        List<ContractAnalysisResult.DetectedClause> detectedClauses,
        List<ContractAnalysisResult.AiSuggestion> aiSuggestions,
        List<String> riskRationale,
        String rawPayload,
        String engine,
        boolean success
    ) {
        public static MlInferenceResult empty() {
            return new MlInferenceResult(List.of(), List.of(), List.of(), null, "none", false);
        }
    }
}

