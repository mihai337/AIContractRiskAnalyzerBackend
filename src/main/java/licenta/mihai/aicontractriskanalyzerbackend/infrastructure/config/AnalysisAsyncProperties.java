package licenta.mihai.aicontractriskanalyzerbackend.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.analysis.async")
@Getter
@Setter
public class AnalysisAsyncProperties {

    private int corePoolSize = 2;
    private int maxPoolSize = 4;
    private int queueCapacity = 100;
}

