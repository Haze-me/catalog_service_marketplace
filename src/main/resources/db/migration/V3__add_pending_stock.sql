-- ============================================================
-- V3: Pending stock buffer
-- Inventory (inventory.updated) and product (product.created) events arrive
-- on separate topics with no cross-topic ordering guarantee. If an inventory
-- event is processed before its product exists in the read model, the stock
-- status would be lost. This table buffers such stock statuses so they are
-- applied as soon as the product is created.
-- ============================================================

CREATE TABLE catalog_pending_stock (
    product_id UUID PRIMARY KEY,
    in_stock   BOOLEAN NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
