CREATE TABLE menu_audit_result (
    id UUID PRIMARY KEY,
    menu_version_id VARCHAR(255) NOT NULL,
    risk_description TEXT NOT NULL,
    severity_score INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_menu_audit_result_version ON menu_audit_result(menu_version_id);
