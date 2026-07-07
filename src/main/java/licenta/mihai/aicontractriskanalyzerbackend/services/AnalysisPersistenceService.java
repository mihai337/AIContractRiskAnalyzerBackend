package licenta.mihai.aicontractriskanalyzerbackend.services;

import java.util.List;

import licenta.mihai.aicontractriskanalyzerbackend.services.EmbeddingStoreService.ClauseEmbeddingRow;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.entity.ContractEntity;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalysisPersistenceService {

    private final ContractRepository contractRepository;
    private final EmbeddingStoreService embeddingStoreService;

    @Transactional
    public void persistAnalysis(ContractEntity entity, List<ClauseEmbeddingRow> embeddingRows) {
        embeddingStoreService.storeClauseEmbeddings(embeddingRows);
        contractRepository.save(entity);
    }

    @Transactional
    public void persistContract(ContractEntity entity) {
        contractRepository.save(entity);
    }
}
