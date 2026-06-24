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

            // One rule per clause type. Selecting a rule means "check the contract for this
            // clause type": if found it's analysed (against the policy text below), if not it's
            // reported as missing. The "policy" is the standard the LLM judges the clause against.
            repository.saveAll(List.of(
                rule("rule_confidentiality", "Confidentiality clause", "Confidential information should be protected by clear non-disclosure obligations with a defined duration.", ClauseType.CONFIDENTIALITY, RiskLevel.HIGH),
                rule("rule_termination", "Termination clause", "The contract should define how and when either party may terminate, including notice periods and consequences.", ClauseType.TERMINATION, RiskLevel.MEDIUM),
                rule("rule_liability", "Liability clause", "Liability should be limited and capped (for example to fees paid); unlimited or uncapped liability is high risk.", ClauseType.LIABILITY, RiskLevel.HIGH),
                rule("rule_payment", "Payment terms clause", "The contract should state payment amounts, timing (for example net 30 days) and consequences for late payment.", ClauseType.PAYMENT, RiskLevel.MEDIUM),
                rule("rule_data_protection", "Data protection clause", "For contracts involving personal data, GDPR compliance and a data processing agreement should be required.", ClauseType.DATA_PROTECTION, RiskLevel.HIGH),
                rule("rule_force_majeure", "Force majeure clause", "The contract should excuse performance for events beyond a party's reasonable control.", ClauseType.FORCE_MAJEURE, RiskLevel.LOW),
                rule("rule_governing_law", "Governing law clause", "The contract should specify the governing law and jurisdiction.", ClauseType.GOVERNING_LAW, RiskLevel.MEDIUM),
                rule("rule_intellectual_property", "Intellectual property clause", "IP ownership and licensing should be clearly assigned and protected.", ClauseType.INTELLECTUAL_PROPERTY, RiskLevel.HIGH),
                rule("rule_dispute_resolution", "Dispute resolution clause", "The contract should define how disputes are resolved, for example arbitration or a chosen court.", ClauseType.DISPUTE_RESOLUTION, RiskLevel.MEDIUM)
            ));
        };
    }

    private CustomRuleEntity rule(
        String id,
        String name,
        String policy,
        ClauseType requiredClause,
        RiskLevel severity
    ) {
        CustomRuleEntity entity = new CustomRuleEntity();
        entity.setId(id);
        entity.setName(name);
        // Stored in the description column; surfaced to users as the rule's "policy".
        entity.setDescription(policy);
        entity.setRequiredClause(requiredClause);
        entity.setSeverity(severity);
        entity.setEnabled(true);
        return entity;
    }
}

