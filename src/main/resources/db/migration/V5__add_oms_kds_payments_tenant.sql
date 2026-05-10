-- =====================================================
-- V5: Order Management System + Kitchen Display + Payments + Multi-Tenant
-- =====================================================

-- Menu availability (86ing support)
ALTER TABLE menu_item ADD COLUMN IF NOT EXISTS available BOOLEAN DEFAULT TRUE;
ALTER TABLE menu_item ADD COLUMN IF NOT EXISTS quantity_remaining INT;

-- Orders
CREATE TABLE restaurant_order (
    id UUID PRIMARY KEY,
    order_number SERIAL,
    channel VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    table_number VARCHAR(10),
    server_name VARCHAR(100),
    guest_count INT,
    subtotal DECIMAL(10,2) NOT NULL DEFAULT 0,
    tax DECIMAL(10,2) NOT NULL DEFAULT 0,
    tip DECIMAL(10,2) NOT NULL DEFAULT 0,
    total DECIMAL(10,2) NOT NULL DEFAULT 0,
    idempotency_key VARCHAR(255) UNIQUE,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_item (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES restaurant_order(id) ON DELETE CASCADE,
    menu_item_id UUID NOT NULL REFERENCES menu_item(id),
    menu_item_name VARCHAR(255) NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    unit_price DECIMAL(10,2) NOT NULL DEFAULT 0,
    line_total DECIMAL(10,2) NOT NULL DEFAULT 0,
    modifier_selections JSONB,
    special_instructions TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    station_routing VARCHAR(50)
);

-- Kitchen Display System
CREATE TABLE kitchen_station (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    display_order INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE
);

CREATE TABLE kitchen_ticket (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES restaurant_order(id) ON DELETE CASCADE,
    order_number BIGINT,
    station_id UUID NOT NULL REFERENCES kitchen_station(id),
    station_name VARCHAR(100),
    status VARCHAR(30) NOT NULL DEFAULT 'NEW',
    items JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    acknowledged_at TIMESTAMP,
    completed_at TIMESTAMP
);

-- Payments
CREATE TABLE payment (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES restaurant_order(id),
    amount DECIMAL(10,2) NOT NULL,
    tip_amount DECIMAL(10,2) DEFAULT 0,
    total_charged DECIMAL(10,2) DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'INITIATED',
    payment_method VARCHAR(30),
    transaction_ref VARCHAR(255),
    failure_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Multi-tenant
CREATE TABLE restaurant (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    timezone VARCHAR(50) DEFAULT 'America/New_York',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE location (
    id UUID PRIMARY KEY,
    restaurant_id UUID NOT NULL REFERENCES restaurant(id),
    name VARCHAR(255) NOT NULL,
    address TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Default kitchen stations
INSERT INTO kitchen_station (id, name, display_order) VALUES
    ('a0000000-0000-0000-0000-000000000001', 'Grill', 1),
    ('a0000000-0000-0000-0000-000000000002', 'Fry', 2),
    ('a0000000-0000-0000-0000-000000000003', 'Salad/Cold', 3),
    ('a0000000-0000-0000-0000-000000000004', 'Bar', 4),
    ('a0000000-0000-0000-0000-000000000005', 'Expo', 5);

-- Indexes
CREATE INDEX idx_order_status ON restaurant_order(status);
CREATE INDEX idx_order_channel ON restaurant_order(channel);
CREATE INDEX idx_order_created ON restaurant_order(created_at);
CREATE INDEX idx_order_item_order ON order_item(order_id);
CREATE INDEX idx_ticket_station ON kitchen_ticket(station_id);
CREATE INDEX idx_ticket_status ON kitchen_ticket(status);
CREATE INDEX idx_payment_order ON payment(order_id);
CREATE INDEX idx_payment_status ON payment(status);
CREATE INDEX idx_location_restaurant ON location(restaurant_id);
