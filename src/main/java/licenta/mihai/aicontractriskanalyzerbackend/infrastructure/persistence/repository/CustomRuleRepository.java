package licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.entity.CustomRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomRuleRepository extends JpaRepository<CustomRuleEntity, String> {

    List<CustomRuleEntity> findAllByEnabledTrue();

    List<CustomRuleEntity> findAllByIdIn(Collection<String> ids);
}

