package licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.repository;

import java.util.List;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.entity.ClauseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClauseRepository extends JpaRepository<ClauseEntity, String> {
    List<ClauseEntity> findByContractId(String contractId);

    long deleteByContractId(String contractId);
}
