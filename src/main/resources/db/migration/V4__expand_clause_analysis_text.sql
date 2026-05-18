ALTER TABLE clause_analysis
    ALTER COLUMN summary TYPE TEXT,
    ALTER COLUMN recommendation TYPE TEXT;

ALTER TABLE detected_issues
    ALTER COLUMN explanation TYPE TEXT,
    ALTER COLUMN highlighted_text TYPE TEXT;
