package licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.repository;

import java.util.List;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.entity.ClauseAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClauseAnalysisRepository extends JpaRepository<ClauseAnalysisEntity, String> {
    List<ClauseAnalysisEntity> findByClauseId(String clauseId);
}

