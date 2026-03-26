package licenta.mihai.aicontractriskanalyzerbackend.application.service;

import java.util.List;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.entity.CustomRuleEntity;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.repository.CustomRuleRepository;
import licenta.mihai.aicontractriskanalyzerbackend.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RuleService {

    private final CustomRuleRepository customRuleRepository;

    public RuleService(CustomRuleRepository customRuleRepository) {
        this.customRuleRepository = customRuleRepository;
    }

    @Transactional(readOnly = true)
    public List<CustomRuleEntity> listRules() {
        return customRuleRepository.findAll();
    }

    @Transactional
    public CustomRuleEntity upsert(CustomRuleEntity entity) {
        return customRuleRepository.save(entity);
    }

    @Transactional
    public void setEnabled(String ruleId, boolean enabled) {
        CustomRuleEntity existing = customRuleRepository.findById(ruleId)
            .orElseThrow(() -> new NotFoundException("Rule not found: " + ruleId));
        existing.setEnabled(enabled);
    }

    @Transactional(readOnly = true)
    public List<CustomRuleEntity> resolveRules(List<String> selectedRuleIds) {
        if (selectedRuleIds == null || selectedRuleIds.isEmpty()) {
            return customRuleRepository.findAllByEnabledTrue();
        }
        return customRuleRepository.findAllByIdIn(selectedRuleIds).stream().filter(CustomRuleEntity::isEnabled).toList();
    }
}

