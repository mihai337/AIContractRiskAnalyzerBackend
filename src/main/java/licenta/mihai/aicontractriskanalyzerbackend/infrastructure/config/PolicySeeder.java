package licenta.mihai.aicontractriskanalyzerbackend.infrastructure.config;

import licenta.mihai.aicontractriskanalyzerbackend.application.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PolicySeeder implements ApplicationRunner {

    private final PolicyService policyService;

    @Override
    public void run(ApplicationArguments args) {
        policyService.seedDefaultsIfMissing();
    }
}
