package licenta.mihai.aicontractriskanalyzerbackend.infrastructure.config;

import java.util.List;
import licenta.mihai.aicontractriskanalyzerbackend.models.ClauseType;
import licenta.mihai.aicontractriskanalyzerbackend.models.RiskLevel;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.entity.CustomRuleEntity;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.repository.CustomRuleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RuleSeedConfig {

    @Bean
    public CommandLineRunner seedRules(CustomRuleRepository repository) {
        return args -> {
            if (repository.count() > 0) {
                return;
            }

            repository.saveAll(List.of(
                rule("rule_confidentiality", "Confidentiality Required", "Contract should contain confidentiality protections", ClauseType.CONFIDENTIALITY, null, RiskLevel.HIGH),
                rule("rule_payment_terms", "Payment Terms", "Contract should include explicit payment terms", ClauseType.PAYMENT, "payment", RiskLevel.MEDIUM),
                rule("rule_gdpr", "GDPR Mention", "For data-bearing contracts, GDPR language is recommended", ClauseType.DATA_PROTECTION, "gdpr", RiskLevel.HIGH)
            ));
        };
    }

    private CustomRuleEntity rule(
        String id,
        String name,
        String description,
        ClauseType requiredClause,
        String keyword,
        RiskLevel severity
    ) {
        CustomRuleEntity entity = new CustomRuleEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setDescription(description);
        entity.setRequiredClause(requiredClause);
        entity.setKeyword(keyword);
        entity.setSeverity(severity);
        entity.setEnabled(true);
        return entity;
    }
}

