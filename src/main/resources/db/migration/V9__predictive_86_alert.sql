-- =====================================================
-- V9: Predictive 86 Alerts table
-- =====================================================

CREATE TABLE predictive_86_alert (
    id                UUID PRIMARY KEY,
    menu_item_id      UUID NOT NULL,
    item_name         VARCHAR(255) NOT NULL,
    trigger_order_id  UUID NOT NULL,
    current_stock     INT NOT NULL,
    predicted_demand  DECIMAL(10,2) NOT NULL,
    risk_score        DECIMAL(10,2) NOT NULL,
    alert_message     VARCHAR(500) NOT NULL,
    status            VARCHAR(50) NOT NULL DEFAULT 'UNRESOLVED', -- UNRESOLVED, ACKNOWLEDGED, AUTO_86D
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at       TIMESTAMP
);

CREATE INDEX idx_p86a_item ON predictive_86_alert(menu_item_id);
CREATE INDEX idx_p86a_status ON predictive_86_alert(status);
CREATE INDEX idx_p86a_created ON predictive_86_alert(created_at);
