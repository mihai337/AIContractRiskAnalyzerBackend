package licenta.mihai.aicontractriskanalyzerbackend.infrastructure.ml;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import licenta.mihai.aicontractriskanalyzerbackend.models.ClauseType;
import licenta.mihai.aicontractriskanalyzerbackend.models.ContractAnalysisResult;
import licenta.mihai.aicontractriskanalyzerbackend.models.RiskLevel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class FastApiMlInferenceAdapter implements MlInferencePort {

    private final MlClientProperties properties;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    @Override
    public MlExtractedTextResult extractText(String contractId, String fileName, String mimeType, String base64Content) {
        try {
            RestClient restClient = buildClient();
            MlExtractResponse response = restClient.post()
                .uri(properties.getExtractTextPath())
                .headers(headers -> applyHeaders(headers, contractId))
                .body(new MlAnalyzeRequest(contractId, fileName, mimeType, base64Content))
                .retrieve()
                .body(MlExtractResponse.class);

            if (response == null) {
                return MlExtractedTextResult.empty();
            }

            String text = response.getText() == null ? "" : response.getText();
            String engine = response.getExtractionEngine() == null ? "fastapi" : response.getExtractionEngine();
            return new MlExtractedTextResult(
                text,
                engine,
                Boolean.TRUE.equals(response.getContainsScannedPages()),
                true
            );
        } catch (Exception ex) {
            log.warn("ML extraction call failed for contract {}: {}", contractId, ex.getMessage());
            return MlExtractedTextResult.empty();
        }
    }

    @Override
    public MlInferenceResult analyzeContract(
        String contractId,
        String fileName,
        String mimeType,
        String base64Content
    ) {
        try {
            RestClient restClient = buildClient();
            MlAnalyzeResponse response = restClient.post()
                .uri(properties.getAnalyzeTextPath())
                .headers(headers -> applyHeaders(headers, contractId))
                .body(new MlAnalyzeRequest(contractId, fileName, mimeType, base64Content))
                .retrieve()
                .body(MlAnalyzeResponse.class);

            if (response == null) {
                return failOrEmpty("ML service returned empty response");
            }

            return new MlInferenceResult(
                toDetectedClauses(response.getDetectedClauses()),
                toSuggestions(response.getAiSuggestions()),
                response.getRiskRationale() == null ? List.of() : response.getRiskRationale(),
                response.getExtractedText() == null ? "" : response.getExtractedText(),
                toRawPayload(response),
                response.getModelMetadata() == null ? "fastapi" : response.getModelMetadata().getEngine(),
                true,
                response.getContractType() == null ? "UNKNOWN" : response.getContractType(),
                response.getContractTypeConfidence() == null ? 0.0 : response.getContractTypeConfidence(),
                response.getIsContract() == null ? Boolean.TRUE : response.getIsContract(),
                response.getNonContractReason()
            );
        } catch (Exception ex) {
            log.warn("ML analysis call failed for contract {}: {}", contractId, ex.getMessage());
            return failOrEmpty(ex.getMessage());
        }
    }

    @Override
    public List<List<Double>> embedTexts(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        try {
            RestClient restClient = buildClient();
            EmbeddingResponse response = restClient.post()
                .uri(properties.getEmbeddingPath())
                .headers(headers -> applyHeaders(headers, "embedding"))
                .body(new EmbeddingRequest(texts))
                .retrieve()
                .body(EmbeddingResponse.class);
            if (response == null || response.getEmbeddings() == null) {
                return List.of();
            }
            return response.getEmbeddings();
        } catch (Exception ex) {
            log.warn("ML embedding call failed: {}", ex.getMessage());
            if (!properties.isFailOpen()) {
                throw new IllegalStateException("ML embedding failed: " + ex.getMessage(), ex);
            }
            return List.of();
        }
    }

    private RestClient buildClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));

        return RestClient.builder()
            .baseUrl(properties.getBaseUrl())
            .requestFactory(requestFactory)
            .build();
    }

    private void applyHeaders(HttpHeaders headers, String contractId) {
        headers.add("X-Correlation-Id", contractId);
        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            headers.add("X-Api-Key", properties.getApiKey());
        }
    }

    private MlInferenceResult failOrEmpty(String reason) {
        if (!properties.isFailOpen()) {
            throw new IllegalStateException("ML analysis failed: " + reason);
        }
        return new MlInferenceResult(List.of(), List.of(), List.of("ML unavailable: " + reason), "", null, "fastapi", false, "UNKNOWN", 0.0, Boolean.TRUE, null);
    }

    private String toRawPayload(MlAnalyzeResponse response) {
        try {
            return OBJECT_MAPPER.writeValueAsString(response);
        } catch (JsonProcessingException ex) {
            return "{\"error\":\"unable to serialize ml payload\"}";
        }
    }

    private List<ContractAnalysisResult.DetectedClause> toDetectedClauses(List<MlClause> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        List<ContractAnalysisResult.DetectedClause> output = new ArrayList<>();
        for (MlClause mlClause : source) {
            output.add(new ContractAnalysisResult.DetectedClause(
                UUID.randomUUID().toString(),
                toClauseType(mlClause.getType()),
                mlClause.getTitle() == null ? "ML detected clause" : mlClause.getTitle(),
                mlClause.getSnippet() == null ? "" : mlClause.getSnippet(),
                normalizeConfidence(mlClause.getConfidence()),
                toRiskLevel(mlClause.getRiskLevel())
            ));
        }
        return output;
    }

    private List<ContractAnalysisResult.AiSuggestion> toSuggestions(List<MlSuggestion> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        List<ContractAnalysisResult.AiSuggestion> output = new ArrayList<>();
        for (MlSuggestion suggestion : source) {
            output.add(new ContractAnalysisResult.AiSuggestion(
                UUID.randomUUID().toString(),
                suggestion.getTitle() == null ? "ML suggestion" : suggestion.getTitle(),
                suggestion.getDescription() == null ? "" : suggestion.getDescription(),
                toRiskLevel(suggestion.getPriority())
            ));
        }
        return output;
    }

    private double normalizeConfidence(Double confidence) {
        if (confidence == null) {
            return 0.5;
        }
        if (confidence < 0) {
            return 0;
        }
        return Math.min(1, confidence);
    }

    private ClauseType toClauseType(String value) {
        if (value == null || value.isBlank()) {
            return ClauseType.OTHER;
        }
        try {
            return ClauseType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ClauseType.OTHER;
        }
    }

    private RiskLevel toRiskLevel(String value) {
        if (value == null || value.isBlank()) {
            return RiskLevel.MEDIUM;
        }
        try {
            return RiskLevel.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return RiskLevel.MEDIUM;
        }
    }

    private record MlAnalyzeRequest(
        String contractId,
        String fileName,
        String mimeType,
        String base64Content
    ) {
    }

    @Data
    private static class MlAnalyzeResponse {
        private List<MlClause> detectedClauses;
        private List<MlSuggestion> aiSuggestions;
        private List<String> riskRationale;
        private String extractedText;
        private Boolean containsScannedPages;
        private String extractionEngine;
        private String keyClauses;
        private String riskAssessmentReport;
        private String recommendedActions;
        private MlModelMetadata modelMetadata;
        private String contractType;
        private Double contractTypeConfidence;
        private Boolean isContract;
        private String nonContractReason;
    }

    @Data
    private static class MlClause {
        private String type;
        private String title;
        private String snippet;
        private Double confidence;
        private String riskLevel;
    }

    @Data
    private static class MlSuggestion {
        private String title;
        private String description;
        private String priority;
    }

    @Data
    private static class MlModelMetadata {
        private String engine;
        private String modelVersion;
        private Long latencyMs;
    }

    @Data
    private static class MlExtractResponse {
        private String text;
        private String extractionEngine;
        private Boolean containsScannedPages;
    }

    private record EmbeddingRequest(
        List<String> texts
    ) {
    }

    @Data
    private static class EmbeddingResponse {
        private List<List<Double>> embeddings;
    }
}
