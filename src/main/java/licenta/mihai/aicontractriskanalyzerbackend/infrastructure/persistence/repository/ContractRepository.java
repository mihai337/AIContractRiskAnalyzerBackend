package licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.repository;

import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.entity.ContractEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractRepository extends JpaRepository<ContractEntity, String> {
}

