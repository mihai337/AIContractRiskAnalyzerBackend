package licenta.mihai.aicontractriskanalyzerbackend.infrastructure.ml;

import java.util.List;
import licenta.mihai.aicontractriskanalyzerbackend.application.port.MlInferencePort;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.ContractAnalysisResult;
import org.springframework.stereotype.Component;

@Component
public class NoopMlInferenceAdapter implements MlInferencePort {

    @Override
    public List<ContractAnalysisResult.DetectedClause> refineDetectedClauses(String extractedText) {
        // ML integration placeholder: return empty list for deterministic MVP.
        return List.of();
    }
}

