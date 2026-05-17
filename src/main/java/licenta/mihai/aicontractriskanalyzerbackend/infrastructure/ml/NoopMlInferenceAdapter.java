package licenta.mihai.aicontractriskanalyzerbackend.infrastructure.ml;

import licenta.mihai.aicontractriskanalyzerbackend.application.port.MlInferencePort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "app.ml", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoopMlInferenceAdapter implements MlInferencePort {

    @Override
    public MlExtractedTextResult extractText(String contractId, String fileName, String mimeType, String base64Content) {
        return MlExtractedTextResult.empty();
    }

    @Override
    public MlInferenceResult analyzeContract(
        String contractId,
        String fileName,
        String mimeType,
        String base64Content
    ) {
        // Deterministic fallback for environments where ML is disabled.
        return MlInferenceResult.empty();
    }

    @Override
    public List<List<Double>> embedTexts(List<String> texts) {
        return List.of();
    }
}
