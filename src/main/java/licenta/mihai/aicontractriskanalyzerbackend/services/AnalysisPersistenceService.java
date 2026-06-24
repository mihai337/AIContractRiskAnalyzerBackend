package licenta.mihai.aicontractriskanalyzerbackend.services;

import java.time.Instant;
import java.util.List;

import licenta.mihai.aicontractriskanalyzerbackend.models.ContractAnalysisResult;
import licenta.mihai.aicontractriskanalyzerbackend.services.EmbeddingStoreService.ClauseEmbeddingRow;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.entity.ContractEntity;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists the results of a contract analysis in a single short-lived transaction.
 * All ML/LLM network calls happen <em>before</em> this is invoked so the database
 * connection is never held open across slow remote calls.
 */
@Service
@RequiredArgsConstructor
public class AnalysisPersistenceService {

    private final ContractRepository contractRepository;
    private final ClausePersistenceService clausePersistenceService;
    private final EmbeddingStoreService embeddingStoreService;

    @Transactional
    public void persistAnalysis(
        ContractEntity entity,
        List<ContractAnalysisResult.DetectedClause> clauses,
        List<ClauseEmbeddingRow> embeddingRows,
        List<LlmAnalysisService.ClauseRiskResult> llmResults,
        Instant now
    ) {
        clausePersistenceService.storeClauses(entity.getId(), clauses, now);
        embeddingStoreService.storeClauseEmbeddings(embeddingRows);
        clausePersistenceService.storeAnalyses(llmResults, now);
        contractRepository.save(entity);
    }

    @Transactional
    public void persistContract(ContractEntity entity) {
        contractRepository.save(entity);
    }
}
