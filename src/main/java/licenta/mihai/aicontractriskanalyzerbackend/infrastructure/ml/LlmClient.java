package licenta.mihai.aicontractriskanalyzerbackend.infrastructure.ml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlmClient {

    private final LlmClientProperties properties;

    private static final String LLM_SCHEMA_NAME = "clause_risk_response";
    private static final Map<String, Object> LLM_SCHEMA = Map.of(
        "type", "object",
        "properties", Map.of(
            "riskLevel", Map.of("type", "string"),
            "riskScore", Map.of("type", "integer"),
            "confidence", Map.of("type", "number"),
            "summary", Map.of("type", "string"),
            "recommendation", Map.of("type", "string"),
            "issues", Map.of(
                "type", "array",
                "items", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "issueType", Map.of("type", "string"),
                        "severity", Map.of("type", "string"),
                        "explanation", Map.of("type", "string"),
                        "highlightedText", Map.of("type", "string")
                    ),
                    "required", List.of("issueType", "severity", "explanation", "highlightedText")
                )
            )
        ),
        "required", List.of("riskLevel", "riskScore", "confidence", "summary", "recommendation", "issues")
    );

    public String completeJson(String prompt) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("LLM is disabled");
        }
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalStateException("Missing LLM API key");
        }
        try {
            RestClient client = buildClient();
            ChatCompletionResponse response = client.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(this::applyHeaders)
                .body(new ChatCompletionRequest(
                    properties.getModel(),
                    List.of(
                        new ChatRequestMessage("system", "You are a legal contract risk analysis engine."),
                        new ChatRequestMessage("user", prompt)
                    ),
                    properties.getTemperature(),
                    properties.getMaxOutputTokens(),
                    ResponseFormat.jsonSchema(LLM_SCHEMA_NAME, LLM_SCHEMA)
                ))
                .retrieve()
                .body(ChatCompletionResponse.class);

            if (response == null) {
                throw new IllegalStateException("LLM response is empty");
            }
            return response.firstContent();
        } catch (Exception ex) {
            log.warn("LLM call failed: {}", ex.getMessage());
            throw new IllegalStateException("LLM call failed", ex);
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

    private void applyHeaders(HttpHeaders headers) {
        headers.setBearerAuth(properties.getApiKey());
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
    }

    @Data
    private static class ChatCompletionRequest {
        private final String model;
        private final List<ChatRequestMessage> messages;
        private final double temperature;
        private final int max_tokens;
        private final ResponseFormat response_format;
    }

    @Data
    private static class ChatRequestMessage {
        private final String role;
        private final String content;
    }

    @Data
    private static class ResponseFormat {
        private final String type;
        private final JsonSchema json_schema;

        private static ResponseFormat jsonSchema(String name, Map<String, Object> schema) {
            return new ResponseFormat("json_schema", new JsonSchema(name, true, schema));
        }
    }

    @Data
    private static class JsonSchema {
        private final String name;
        private final boolean strict;
        private final Map<String, Object> schema;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChatCompletionResponse(List<Choice> choices) {
        String firstContent() {
            if (choices == null || choices.isEmpty()) {
                return "";
            }
            Choice choice = choices.get(0);
            if (choice == null || choice.message == null) {
                return "";
            }
            String content = choice.message.content;
            if (content == null || content.isBlank()) {
                return choice.message.reasoning_content == null ? "" : choice.message.reasoning_content;
            }
            return content;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    private static class Choice {
        private ChatResponseMessage message;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    private static class ChatResponseMessage {
        private String role;
        private String content;
        private String reasoning_content;
    }
}
