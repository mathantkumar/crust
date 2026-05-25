-- =====================================================
-- V8: Demand forecast storage table
-- =====================================================

CREATE TABLE item_demand_forecast (
    id                UUID PRIMARY KEY,
    restaurant_id     UUID NOT NULL,
    location_id       UUID NOT NULL,
    menu_item_id      UUID NOT NULL,
    item_name         VARCHAR(255) NOT NULL,
    forecast_date     DATE NOT NULL,
    hour_of_day       SMALLINT NOT NULL,         -- 0-23
    day_of_week       SMALLINT NOT NULL,         -- 1(Mon)-7(Sun)
    predicted_quantity DECIMAL(10,2) NOT NULL DEFAULT 0,
    predicted_revenue  DECIMAL(12,2) NOT NULL DEFAULT 0,
    confidence         DECIMAL(5,4) NOT NULL DEFAULT 0, -- 0.0000 - 1.0000
    model_version      VARCHAR(50) NOT NULL,
    generated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Lookup: what's the forecast for a given item on a given date/hour?
CREATE UNIQUE INDEX idx_idf_upsert
    ON item_demand_forecast(restaurant_id, location_id, menu_item_id, forecast_date, hour_of_day);

CREATE INDEX idx_idf_date        ON item_demand_forecast(forecast_date);
CREATE INDEX idx_idf_item        ON item_demand_forecast(menu_item_id);
CREATE INDEX idx_idf_model       ON item_demand_forecast(model_version);
CREATE INDEX idx_idf_generated   ON item_demand_forecast(generated_at);
