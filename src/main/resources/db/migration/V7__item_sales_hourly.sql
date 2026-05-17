-- =====================================================
-- V7: Hourly item sales aggregate table for analytics/ML
-- =====================================================

CREATE TABLE item_sales_hourly (
    id              UUID PRIMARY KEY,
    restaurant_id   UUID REFERENCES restaurant(id),
    location_id     UUID REFERENCES location(id),
    menu_item_id    UUID NOT NULL,
    item_name       VARCHAR(255) NOT NULL,
    category_name   VARCHAR(255),
    sales_date      DATE NOT NULL,
    hour_of_day     SMALLINT NOT NULL,       -- 0-23
    day_of_week     SMALLINT NOT NULL,       -- 1(Mon)-7(Sun)
    quantity_sold   INT NOT NULL DEFAULT 0,
    gross_revenue   DECIMAL(12,2) NOT NULL DEFAULT 0,
    modifier_revenue DECIMAL(12,2) NOT NULL DEFAULT 0,
    order_count     INT NOT NULL DEFAULT 0,
    avg_unit_price  DECIMAL(10,2) NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Composite index for the upsert lookup (one row per item per hour per location)
CREATE UNIQUE INDEX idx_ish_upsert
    ON item_sales_hourly(restaurant_id, location_id, menu_item_id, sales_date, hour_of_day);

-- Query patterns: time-series, item drill-down, category roll-up
CREATE INDEX idx_ish_date_hour     ON item_sales_hourly(sales_date, hour_of_day);
CREATE INDEX idx_ish_item          ON item_sales_hourly(menu_item_id);
CREATE INDEX idx_ish_category      ON item_sales_hourly(category_name);
CREATE INDEX idx_ish_restaurant    ON item_sales_hourly(restaurant_id);
CREATE INDEX idx_ish_day_of_week   ON item_sales_hourly(day_of_week);

-- Track which orders have been aggregated so we don't double-count
ALTER TABLE restaurant_order
    ADD COLUMN IF NOT EXISTS aggregated BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_order_aggregated ON restaurant_order(aggregated) WHERE NOT aggregated;
