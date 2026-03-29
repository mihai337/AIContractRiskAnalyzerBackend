package licenta.mihai.aicontractriskanalyzerbackend.infrastructure.ml;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import licenta.mihai.aicontractriskanalyzerbackend.application.port.MlInferencePort;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.ClauseType;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.ContractAnalysisResult;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.RiskLevel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "app.ml", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class FastApiMlInferenceAdapter implements MlInferencePort {

    private final MlClientProperties properties;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    @Override
    public MlInferenceResult analyzeContract(String contractId, String extractedText) {
        try {
            RestClient restClient = buildClient();
            MlAnalyzeResponse response = restClient.post()
                .uri(URI.create(properties.getAnalyzeTextPath()))
                .headers(headers -> applyHeaders(headers, contractId))
                .body(new MlAnalyzeRequest(contractId, extractedText))
                .retrieve()
                .body(MlAnalyzeResponse.class);

            if (response == null) {
                return failOrEmpty("ML service returned empty response");
            }

            return new MlInferenceResult(
                toDetectedClauses(response.detectedClauses),
                toSuggestions(response.aiSuggestions),
                response.riskRationale == null ? List.of() : response.riskRationale,
                toRawPayload(response),
                response.modelMetadata == null ? "fastapi" : response.modelMetadata.engine,
                true
            );
        } catch (Exception ex) {
            log.warn("ML analysis call failed for contract {}: {}", contractId, ex.getMessage());
            return failOrEmpty(ex.getMessage());
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
        return new MlInferenceResult(List.of(), List.of(), List.of("ML unavailable: " + reason), null, "fastapi", false);
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
                toClauseType(mlClause.type),
                mlClause.title == null ? "ML detected clause" : mlClause.title,
                mlClause.snippet == null ? "" : mlClause.snippet,
                normalizeConfidence(mlClause.confidence),
                toRiskLevel(mlClause.riskLevel)
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
                suggestion.title == null ? "ML suggestion" : suggestion.title,
                suggestion.description == null ? "" : suggestion.description,
                toRiskLevel(suggestion.priority)
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

    private record MlAnalyzeRequest(String contractId, String text) {
    }

    private static class MlAnalyzeResponse {
        public List<MlClause> detectedClauses;
        public List<MlSuggestion> aiSuggestions;
        public List<String> riskRationale;
        public String keyClauses;
        public String riskAssessmentReport;
        public String recommendedActions;
        public MlModelMetadata modelMetadata;
    }

    private static class MlClause {
        public String type;
        public String title;
        public String snippet;
        public Double confidence;
        public String riskLevel;
    }

    private static class MlSuggestion {
        public String title;
        public String description;
        public String priority;
    }

    private static class MlModelMetadata {
        public String engine;
        public String modelVersion;
        public Long latencyMs;
    }
}

