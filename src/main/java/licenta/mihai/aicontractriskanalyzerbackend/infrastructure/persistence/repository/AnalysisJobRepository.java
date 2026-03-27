package licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.repository;

import java.util.Optional;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.entity.AnalysisJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisJobRepository extends JpaRepository<AnalysisJobEntity, String> {

    Optional<AnalysisJobEntity> findByIdAndContractId(String id, String contractId);
}

