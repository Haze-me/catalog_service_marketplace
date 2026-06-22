-- ============================================================
-- V2: Add index on brand for filterByBrand queries
-- ============================================================

CREATE INDEX idx_catalog_products_brand ON catalog_products(LOWER(brand));