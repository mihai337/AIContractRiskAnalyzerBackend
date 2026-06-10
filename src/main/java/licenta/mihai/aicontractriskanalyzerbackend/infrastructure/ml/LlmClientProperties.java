package licenta.mihai.aicontractriskanalyzerbackend.infrastructure.ml;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.llm")
@Getter
@Setter
public class LlmClientProperties {

    private boolean enabled = true;
    private String baseUrl = "http://localhost:1234/v1";
    private String apiKey;
    private String model = "qwen/qwen3.5-9b";
    private double temperature = 0.2;
    private int maxOutputTokens = 800;
    private boolean failOpen = false;
    private long connectTimeoutMs = 5000;
    private long readTimeoutMs = 120000;
}
