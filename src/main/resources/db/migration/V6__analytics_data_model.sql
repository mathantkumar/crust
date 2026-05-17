-- =====================================================
-- V6: Analytics Data Model — Tenant context on orders,
--     category/version snapshots + timestamps on items,
--     structured modifier pricing
-- =====================================================

-- ─── 1. restaurant_order: add tenant context ─────────────────────────────────

ALTER TABLE restaurant_order
    ADD COLUMN IF NOT EXISTS restaurant_id UUID REFERENCES restaurant(id),
    ADD COLUMN IF NOT EXISTS location_id   UUID REFERENCES location(id);

CREATE INDEX IF NOT EXISTS idx_order_restaurant ON restaurant_order(restaurant_id);
CREATE INDEX IF NOT EXISTS idx_order_location   ON restaurant_order(location_id);

-- ─── 2. order_item: snapshot fields + timestamps ─────────────────────────────

ALTER TABLE order_item
    ADD COLUMN IF NOT EXISTS category_id     UUID,
    ADD COLUMN IF NOT EXISTS category_name   VARCHAR(255),
    ADD COLUMN IF NOT EXISTS menu_version_id UUID,
    ADD COLUMN IF NOT EXISTS created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS completed_at    TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_order_item_category     ON order_item(category_id);
CREATE INDEX IF NOT EXISTS idx_order_item_menu_version ON order_item(menu_version_id);

-- ─── 3. order_item_modifier: structured modifier pricing ─────────────────────

CREATE TABLE IF NOT EXISTS order_item_modifier (
    id            UUID PRIMARY KEY,
    order_item_id UUID NOT NULL REFERENCES order_item(id) ON DELETE CASCADE,
    modifier_id   UUID,                                -- snapshot ref, no FK
    modifier_name VARCHAR(255) NOT NULL,
    price_impact  DECIMAL(10,2) NOT NULL DEFAULT 0,    -- +/- amount
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_oim_order_item ON order_item_modifier(order_item_id);
CREATE INDEX IF NOT EXISTS idx_oim_modifier   ON order_item_modifier(modifier_id);
