package licenta.mihai.aicontractriskanalyzerbackend.infrastructure.persistence.repository;

import java.util.List;
import java.util.stream.Collectors;
import licenta.mihai.aicontractriskanalyzerbackend.domain.model.EmbeddingMatch;
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
        List<Double> embedding
    ) {
        String vectorLiteral = toVectorLiteral(embedding);
        jdbcTemplate.update(
            """
            INSERT INTO clause_embeddings (clause_id, contract_id, clause_type, snippet, embedding)
            VALUES (?, ?, ?, ?, ?::vector)
            ON CONFLICT (clause_id) DO UPDATE
            SET contract_id = EXCLUDED.contract_id,
                clause_type = EXCLUDED.clause_type,
                snippet = EXCLUDED.snippet,
                embedding = EXCLUDED.embedding
            """,
            clauseId,
            contractId,
            clauseType,
            snippet,
            vectorLiteral
        );
    }

    public List<EmbeddingMatch> findSimilar(List<Double> embedding, int limit) {
        String vectorLiteral = toVectorLiteral(embedding);
        return jdbcTemplate.query(
            """
            SELECT clause_id,
                   contract_id,
                   clause_type,
                   snippet,
                   embedding <=> ?::vector AS distance
            FROM clause_embeddings
            ORDER BY embedding <=> ?::vector
            LIMIT ?
            """,
            MATCH_MAPPER,
            vectorLiteral,
            vectorLiteral,
            limit
        );
    }

    private String toVectorLiteral(List<Double> embedding) {
        return embedding.stream()
            .map(value -> String.format("%.6f", value))
            .collect(Collectors.joining(",", "[", "]"));
    }
}

