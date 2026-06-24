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

    public void storeClauseEmbeddings(List<ClauseEmbeddingRow> rows) {
        if (rows == null) {
            return;
        }
        for (ClauseEmbeddingRow row : rows) {
            clauseEmbeddingRepository.saveEmbedding(
                row.clauseId(), row.contractId(), row.clauseType(), row.snippet(), row.embedding(), row.ownerId());
        }
    }

    public List<EmbeddingMatch> findSimilarClauses(List<Double> embedding, String ownerId, int limit) {
        return clauseEmbeddingRepository.findSimilar(embedding, ownerId, limit);
    }

    public record ClauseEmbeddingRow(
        String clauseId,
        String contractId,
        String clauseType,
        String snippet,
        List<Double> embedding,
        String ownerId
    ) {
    }
}

