package licenta.mihai.aicontractriskanalyzerbackend.application.port;

import java.util.List;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.ContractAnalysisResult;

public interface MlInferencePort {

    List<ContractAnalysisResult.DetectedClause> refineDetectedClauses(String extractedText);
}

