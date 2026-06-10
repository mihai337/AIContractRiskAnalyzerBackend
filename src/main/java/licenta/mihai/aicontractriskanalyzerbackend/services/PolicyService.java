package licenta.mihai.aicontractriskanalyzerbackend.services;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import licenta.mihai.aicontractriskanalyzerbackend.models.ClauseType;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.entity.PolicyEntity;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PolicyService {

    private final PolicyRepository policyRepository;

    public String policyForClauseType(ClauseType clauseType, String contractType) {
        if (clauseType == null) {
            return "";
        }
        if (contractType != null && !contractType.isBlank()) {
            Optional<PolicyEntity> scoped = policyRepository
                .findFirstByPolicyTypeIgnoreCaseAndContractTypeIgnoreCase(clauseType.name(), contractType.trim());
            if (scoped.isPresent()) {
                return scoped.get().getContent();
            }
        }
        Optional<PolicyEntity> fallback = policyRepository.findFirstByPolicyTypeIgnoreCaseAndContractTypeIsNull(clauseType.name());
        return fallback.map(PolicyEntity::getContent).orElse("");
    }

    public void seedDefaultsIfMissing() {
        if (policyRepository.count() > 0) {
            return;
        }
        Instant now = Instant.now();
        policyRepository.saveAll(List.of(
            buildPolicy("CONFIDENTIALITY", null, "Confidentiality", "Confidential information must be protected for at least 3 years.", now),
            buildPolicy("LIABILITY", null, "Liability", "Liability must be capped at 12 months of fees.", now),
            buildPolicy("PAYMENT", null, "Payment", "Payment terms should be net 30 days with late fees capped.", now),
            buildPolicy("TERMINATION", null, "Termination", "Termination requires 30 days notice and mutual obligations.", now),
            buildPolicy("GOVERNING_LAW", null, "Governing Law", "Approved jurisdictions are EU member states.", now),
            buildPolicy("DATA_PROTECTION", null, "Data Protection", "GDPR compliance and DPA are required.", now)
        ));
    }

    private PolicyEntity buildPolicy(String type, String contractType, String title, String content, Instant createdAt) {
        PolicyEntity entity = new PolicyEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setPolicyType(type);
        entity.setContractType(contractType);
        entity.setTitle(title);
        entity.setContent(content);
        entity.setCreatedAt(createdAt);
        return entity;
    }
}
