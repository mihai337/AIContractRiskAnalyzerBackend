package licenta.mihai.aicontractriskanalyzerbackend.infrastructure.llm;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.llm")
@Getter
@Setter
public class LlmClientProperties {

    private boolean enabled = true;
    private String baseUrl = "https://api.openai.com/v1";
    private String apiKey;
    private String model = "gpt-4.1-mini";
    private double temperature = 0.2;
    private int maxOutputTokens = 800;
    private boolean failOpen = false;
}

