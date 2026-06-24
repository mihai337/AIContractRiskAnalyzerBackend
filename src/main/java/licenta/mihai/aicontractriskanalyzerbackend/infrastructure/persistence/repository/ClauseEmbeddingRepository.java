package licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.repository;

import java.util.List;
import java.util.stream.Collectors;
import licenta.mihai.aicontractriskanalyzerbackend.models.EmbeddingMatch;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ClauseEmbeddingRepository {

    private static final RowMapper<EmbeddingMatch> MATCH_MAPPER = (rs, rowNum) -> new EmbeddingMatch(
        rs.getString("clause_id"),
        rs.getString("contract_id"),
        rs.getString("clause_type"),
        rs.getString("snippet"),
        rs.getDouble("distance")
    );

    private final JdbcTemplate jdbcTemplate;

    public void saveEmbedding(
        String clauseId,
        String contractId,
        String clauseType,
        String snippet,
        List<Double> embedding,
        String ownerId
    ) {
        String vectorLiteral = toVectorLiteral(embedding);
        jdbcTemplate.update(
            """
            INSERT INTO clause_embeddings (clause_id, contract_id, clause_type, snippet, embedding, owner_id)
            VALUES (?, ?, ?, ?, ?::vector, ?)
            ON CONFLICT (clause_id) DO UPDATE
            SET contract_id = EXCLUDED.contract_id,
                clause_type = EXCLUDED.clause_type,
                snippet = EXCLUDED.snippet,
                embedding = EXCLUDED.embedding,
                owner_id = EXCLUDED.owner_id
            """,
            clauseId,
            contractId,
            clauseType,
            snippet,
            vectorLiteral,
            ownerId
        );
    }

    /**
     * Returns the nearest clauses visible to {@code ownerId}: the caller's own clauses
     * plus shared reference clauses (owner_id IS NULL, e.g. seeded from CUAD).
     */
    public List<EmbeddingMatch> findSimilar(List<Double> embedding, String ownerId, int limit) {
        String vectorLiteral = toVectorLiteral(embedding);
        return jdbcTemplate.query(
            """
            SELECT clause_id,
                   contract_id,
                   clause_type,
                   snippet,
                   embedding <=> ?::vector AS distance
            FROM clause_embeddings
            WHERE owner_id = ? OR owner_id IS NULL
            ORDER BY embedding <=> ?::vector
            LIMIT ?
            """,
            MATCH_MAPPER,
            vectorLiteral,
            ownerId,
            vectorLiteral,
            limit
        );
    }

    public int deleteByContractId(String contractId) {
        return jdbcTemplate.update(
            "DELETE FROM clause_embeddings WHERE contract_id = ?",
            contractId
        );
    }

    private String toVectorLiteral(List<Double> embedding) {
        return embedding.stream()
            .map(value -> String.format("%.6f", value))
            .collect(Collectors.joining(",", "[", "]"));
    }
}
