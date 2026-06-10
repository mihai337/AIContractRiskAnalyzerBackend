package licenta.mihai.aicontractriskanalyzerbackend.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    private int corePoolSize = 2;
    private int maxPoolSize = 4;
    private int queueCapacity = 100;

    @Bean(name = "analysisTaskExecutor")
    public ThreadPoolTaskExecutor analysisTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("analysis-job-");
        executor.initialize();
        return executor;
    }
}

