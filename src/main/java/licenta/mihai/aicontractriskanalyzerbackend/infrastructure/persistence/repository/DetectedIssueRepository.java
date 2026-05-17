package licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.repository;

import java.util.List;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.entity.DetectedIssueEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetectedIssueRepository extends JpaRepository<DetectedIssueEntity, String> {
    List<DetectedIssueEntity> findByClauseAnalysisId(String clauseAnalysisId);
}

