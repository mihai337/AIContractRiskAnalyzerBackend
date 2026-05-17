package licenta.mihai.aicontractriskanalyzerbackend.application.port;

import java.util.List;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.ContractAnalysisResult;

public interface MlInferencePort {

    MlExtractedTextResult extractText(String contractId, String fileName, String mimeType, String base64Content);

    MlInferenceResult analyzeContract(
        String contractId,
        String fileName,
        String mimeType,
        String base64Content
    );

    record MlInferenceResult(
        List<ContractAnalysisResult.DetectedClause> detectedClauses,
        List<ContractAnalysisResult.AiSuggestion> aiSuggestions,
        List<String> riskRationale,
        String extractedText,
        String rawPayload,
        String engine,
        boolean success,
        String contractType,
        Double contractTypeConfidence,
        Boolean isContract,
        String nonContractReason
    ) {
        public static MlInferenceResult empty() {
            return new MlInferenceResult(
                List.of(),
                List.of(),
                List.of(),
                "",
                null,
                "none",
                false,
                "UNKNOWN",
                0.0,
                Boolean.TRUE,
                null
            );
        }
    }

    record MlExtractedTextResult(
        String text,
        String extractionEngine,
        boolean containsScannedPages,
        boolean success
    ) {
        public static MlExtractedTextResult empty() {
            return new MlExtractedTextResult("", "none", false, false);
        }
    }
}
