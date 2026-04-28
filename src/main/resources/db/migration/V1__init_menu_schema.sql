CREATE TABLE menu_version (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL,
    correlation_id VARCHAR(255)
);

CREATE TABLE category (
    id UUID PRIMARY KEY,
    menu_version_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    correlation_id VARCHAR(255),
    CONSTRAINT fk_category_menu_version FOREIGN KEY (menu_version_id) REFERENCES menu_version(id)
);

CREATE TABLE menu_item (
    id UUID PRIMARY KEY,
    category_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    base_price DECIMAL(10, 2),
    correlation_id VARCHAR(255),
    CONSTRAINT fk_menu_item_category FOREIGN KEY (category_id) REFERENCES category(id)
);

CREATE TABLE modifier_group (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    menu_item_id UUID,
    parent_modifier_id UUID,
    correlation_id VARCHAR(255),
    CONSTRAINT chk_modifier_group_parent CHECK (
        (menu_item_id IS NOT NULL AND parent_modifier_id IS NULL) OR
        (menu_item_id IS NULL AND parent_modifier_id IS NOT NULL)
    ),
    CONSTRAINT fk_modifier_group_menu_item FOREIGN KEY (menu_item_id) REFERENCES menu_item(id)
);

CREATE TABLE modifier (
    id UUID PRIMARY KEY,
    modifier_group_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    price_adjustment DECIMAL(10, 2),
    correlation_id VARCHAR(255),
    CONSTRAINT fk_modifier_modifier_group FOREIGN KEY (modifier_group_id) REFERENCES modifier_group(id)
);

-- Add foreign key for parent_modifier_id now that the modifier table exists
ALTER TABLE modifier_group
    ADD CONSTRAINT fk_modifier_group_parent_modifier
    FOREIGN KEY (parent_modifier_id) REFERENCES modifier(id);

CREATE INDEX idx_menu_version_correlation ON menu_version(correlation_id);
CREATE INDEX idx_category_correlation ON category(correlation_id);
CREATE INDEX idx_menu_item_correlation ON menu_item(correlation_id);
CREATE INDEX idx_modifier_group_correlation ON modifier_group(correlation_id);
CREATE INDEX idx_modifier_correlation ON modifier(correlation_id);
