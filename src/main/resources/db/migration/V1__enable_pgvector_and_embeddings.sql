CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS clause_embeddings (
    clause_id TEXT PRIMARY KEY,
    contract_id TEXT NOT NULL,
    clause_type TEXT NOT NULL,
    snippet TEXT NOT NULL,
    embedding VECTOR(384) NOT NULL
);

CREATE INDEX IF NOT EXISTS clause_embeddings_embedding_idx
    ON clause_embeddings USING ivfflat (embedding vector_cosine_ops);

