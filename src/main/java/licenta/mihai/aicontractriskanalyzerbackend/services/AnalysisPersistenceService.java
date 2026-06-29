package licenta.mihai.aicontractriskanalyzerbackend.services;

import java.util.List;

import licenta.mihai.aicontractriskanalyzerbackend.services.EmbeddingStoreService.ClauseEmbeddingRow;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.entity.ContractEntity;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists the results of a contract analysis in a single short-lived transaction.
 * The analysis itself is stored on the contract (its {@code analysis_json} column); only the
 * clause embeddings are written separately (for retrieval). All ML/LLM network calls happen
 * <em>before</em> this is invoked so the database connection is never held open across them.
 */
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
