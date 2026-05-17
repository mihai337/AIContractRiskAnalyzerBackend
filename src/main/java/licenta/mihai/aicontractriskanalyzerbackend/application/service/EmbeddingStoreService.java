package licenta.mihai.aicontractriskanalyzerbackend.application.service;

import java.util.List;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.EmbeddingMatch;
import licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.repository.ClauseEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmbeddingStoreService {

    private final ClauseEmbeddingRepository clauseEmbeddingRepository;

    public void storeClauseEmbedding(
        String clauseId,
        String contractId,
        String clauseType,
        String snippet,
        List<Double> embedding
    ) {
        clauseEmbeddingRepository.saveEmbedding(clauseId, contractId, clauseType, snippet, embedding);
    }

    public List<EmbeddingMatch> findSimilarClauses(List<Double> embedding, int limit) {
        return clauseEmbeddingRepository.findSimilar(embedding, limit);
    }
}

