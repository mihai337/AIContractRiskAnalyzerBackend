package licenta.mihai.aicontractriskanalyzerbackend.infrastructure.ml;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ml")
@Getter
@Setter
public class MlClientProperties {

    private boolean enabled = false;
    private String baseUrl = "http://localhost:8000";
    private String extractTextPath = "/v1/ml/extract-text";
    private String analyzeTextPath = "/v1/ml/analyze-text";
    private String apiKey;
    private boolean failOpen = true;
    private int connectTimeoutMs = 2000;
    private int readTimeoutMs = 120000;
}

