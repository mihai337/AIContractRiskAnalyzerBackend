package licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.repository;

import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.entity.PolicyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PolicyRepository extends JpaRepository<PolicyEntity, String> {
    Optional<PolicyEntity> findFirstByPolicyTypeIgnoreCase(String policyType);

    Optional<PolicyEntity> findFirstByPolicyTypeIgnoreCaseAndContractTypeIgnoreCase(String policyType, String contractType);

    Optional<PolicyEntity> findFirstByPolicyTypeIgnoreCaseAndContractTypeIsNull(String policyType);
}
