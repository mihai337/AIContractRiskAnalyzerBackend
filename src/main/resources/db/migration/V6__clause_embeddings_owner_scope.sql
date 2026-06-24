-- Scope clause-embedding retrieval per user.
-- owner_id = the contract owner for user-uploaded clauses.
-- owner_id IS NULL marks a shared reference clause (e.g. seeded from CUAD) that is
-- retrievable by everyone. Retrieval filters on (owner_id = :owner OR owner_id IS NULL).
ALTER TABLE clause_embeddings ADD COLUMN IF NOT EXISTS owner_id TEXT;

CREATE INDEX IF NOT EXISTS clause_embeddings_owner_id_idx
    ON clause_embeddings (owner_id);
