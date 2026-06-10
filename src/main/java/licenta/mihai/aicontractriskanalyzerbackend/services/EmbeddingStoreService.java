package licenta.mihai.aicontractriskanalyzerbackend.services;

import java.util.List;
import licenta.mihai.aicontractriskanalyzerbackend.models.EmbeddingMatch;
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

