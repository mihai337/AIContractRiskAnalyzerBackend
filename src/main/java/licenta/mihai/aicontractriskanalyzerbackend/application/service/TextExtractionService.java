package licenta.mihai.aicontractriskanalyzerbackend.application.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.ExtractedText;
import licenta.mihai.aicontractriskanalyzerbackend.shared.exception.BadRequestException;
import org.springframework.stereotype.Service;

@Service
public class TextExtractionService {

    public ExtractedText extract(String fileName, String mimeType, String base64Content) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64Content);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid base64Content payload");
        }
        String text = new String(bytes, StandardCharsets.UTF_8);
        boolean likelyScanned = text.trim().isEmpty();

        // Placeholder extractor until OCR/PDF engine is integrated.
        return new ExtractedText(text, "placeholder-base64-decoder", likelyScanned);
    }
}


