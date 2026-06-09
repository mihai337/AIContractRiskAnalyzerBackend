package licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.repository;

import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.entity.ContractEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContractRepository extends JpaRepository<ContractEntity, String> {
    List<ContractEntity> findByOwnerIdOrderByUploadedAtDesc(String ownerId);

    Optional<ContractEntity> findByIdAndOwnerId(String id, String ownerId);

    long deleteByIdAndOwnerId(String id, String ownerId);
}
