package licenta.mihai.aicontractriskanalyzerbackend.infrastructure.ml;

import licenta.mihai.aicontractriskanalyzerbackend.application.port.MlInferencePort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.ml", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoopMlInferenceAdapter implements MlInferencePort {

    @Override
    public MlInferenceResult analyzeContract(String contractId, String extractedText) {
        // Deterministic fallback for environments where ML is disabled.
        return MlInferenceResult.empty();
    }
}

