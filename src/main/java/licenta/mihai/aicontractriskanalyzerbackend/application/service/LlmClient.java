package licenta.mihai.aicontractriskanalyzerbackend.application.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Duration;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.llm.LlmClientProperties;
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

    public String completeJson(String prompt) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("LLM is disabled");
        }
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalStateException("Missing LLM API key");
        }
        try {
            RestClient client = buildClient();
            LlmResponse response = client.post()
                .uri("/responses")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> applyHeaders(headers))
                .body(new LlmRequest(
                    properties.getModel(),
                    prompt,
                    properties.getTemperature(),
                    properties.getMaxOutputTokens(),
                    new ResponseFormat("json_object")
                ))
                .retrieve()
                .body(LlmResponse.class);

            if (response == null) {
                throw new IllegalStateException("LLM response is empty");
            }
            return response.outputText();
        } catch (Exception ex) {
            log.warn("LLM call failed: {}", ex.getMessage());
            throw new IllegalStateException("LLM call failed", ex);
        }
    }

    private RestClient buildClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(60));

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
    private static class LlmRequest {
        private final String model;
        private final String input;
        private final double temperature;
        private final int max_output_tokens;
        private final ResponseFormat response_format;
    }

    @Data
    private static class ResponseFormat {
        private final String type;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LlmResponse(List<LlmOutput> output) {
        String outputText() {
            if (output == null) {
                return "";
            }
            for (LlmOutput item : output) {
                if (item == null || item.content == null) {
                    continue;
                }
                for (LlmContent content : item.content) {
                    if (content != null && content.text != null && !content.text.isBlank()) {
                        return content.text;
                    }
                }
            }
            return "";
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class LlmOutput {
        private List<LlmContent> content;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class LlmContent {
        private String text;
    }
}
