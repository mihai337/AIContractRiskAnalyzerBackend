ALTER TABLE policies
ADD COLUMN IF NOT EXISTS contract_type TEXT;

CREATE INDEX IF NOT EXISTS idx_policies_type_contract
ON policies (policy_type, contract_type);

