ALTER TABLE menu_audit_result
    ADD COLUMN category             VARCHAR(50),
    ADD COLUMN impact_score         INT,
    ADD COLUMN plain_english_summary TEXT,
    ADD COLUMN suggested_action     TEXT;
