package licenta.mihai.aicontractriskanalyzerbackend.application.service;

import lombok.RequiredArgsConstructor;
import licenta.mihai.aicontractriskanalyzerbackend.application.port.MlInferencePort;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.ExtractedText;
import licenta.mihai.aicontractriskanalyzerbackend.shared.exception.BadRequestException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TextExtractionService {

    private final MlInferencePort mlInferencePort;

    public ExtractedText extract(String fileName, String mimeType, String base64Content) {
        MlInferencePort.MlExtractedTextResult mlResult = mlInferencePort.extractText(
            "extract-" + System.currentTimeMillis(),
            fileName,
            mimeType,
            base64Content
        );
        if (!mlResult.success()) {
            throw new BadRequestException("Text extraction failed in ML service");
        }
        return new ExtractedText(mlResult.text(), mlResult.extractionEngine(), mlResult.containsScannedPages());
    }
}


