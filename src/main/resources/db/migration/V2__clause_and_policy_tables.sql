CREATE TABLE IF NOT EXISTS clauses (
    id TEXT PRIMARY KEY,
    contract_id TEXT NOT NULL,
    clause_type TEXT NOT NULL,
    title TEXT NOT NULL,
    snippet TEXT NOT NULL,
    confidence DOUBLE PRECISION NOT NULL,
    risk_level TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS clause_analysis (
    id TEXT PRIMARY KEY,
    clause_id TEXT NOT NULL REFERENCES clauses(id) ON DELETE CASCADE,
    risk_level TEXT NOT NULL,
    risk_score INTEGER NOT NULL,
    confidence DOUBLE PRECISION NOT NULL,
    summary TEXT NOT NULL,
    recommendation TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS detected_issues (
    id TEXT PRIMARY KEY,
    clause_analysis_id TEXT NOT NULL REFERENCES clause_analysis(id) ON DELETE CASCADE,
    issue_type TEXT NOT NULL,
    severity TEXT NOT NULL,
    explanation TEXT NOT NULL,
    highlighted_text TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS policies (
    id TEXT PRIMARY KEY,
    policy_type TEXT NOT NULL,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);

