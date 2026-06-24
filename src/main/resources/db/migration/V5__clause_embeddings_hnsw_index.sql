-- Replace the IVFFlat index with HNSW.
-- HNSW needs no training step (IVFFlat builds poor lists on a small/empty table),
-- gives better recall as the table grows, and supports cosine distance (<=>).
-- Requires pgvector >= 0.5.0.
DROP INDEX IF EXISTS clause_embeddings_embedding_idx;

CREATE INDEX IF NOT EXISTS clause_embeddings_embedding_hnsw_idx
    ON clause_embeddings USING hnsw (embedding vector_cosine_ops);
