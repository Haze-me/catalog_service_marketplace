-- ============================================================
-- V1: Initial catalog schema
-- Catalog Service owns this schema exclusively.
-- Populated entirely via Kafka events — never written to directly
-- by any client request.
-- ============================================================

CREATE TABLE catalog_categories (
                                    category_id UUID PRIMARY KEY,
                                    name VARCHAR(255) NOT NULL,
                                    description TEXT,
                                    slug VARCHAR(255) NOT NULL,
                                    parent_id UUID,
                                    is_active BOOLEAN NOT NULL DEFAULT TRUE,
                                    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                                    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_catalog_categories_slug ON catalog_categories(slug);
CREATE INDEX idx_catalog_categories_parent ON catalog_categories(parent_id);

CREATE TABLE catalog_products (
                                  product_id UUID PRIMARY KEY,
                                  vendor_id UUID NOT NULL,
                                  category_id UUID NOT NULL,
                                  name VARCHAR(255) NOT NULL,
                                  description TEXT,
                                  brand VARCHAR(100),
                                  sku VARCHAR(100) NOT NULL,
                                  price NUMERIC(12, 2) NOT NULL,
                                  slug VARCHAR(300) NOT NULL,
                                  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
                                  primary_image_url VARCHAR(1000),
                                  in_stock BOOLEAN NOT NULL DEFAULT FALSE,
                                  average_rating NUMERIC(3, 2) NOT NULL DEFAULT 0.00,
                                  review_count INTEGER NOT NULL DEFAULT 0,
                                  search_vector TSVECTOR,
                                  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                                  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

                                  CONSTRAINT fk_catalog_products_category
                                      FOREIGN KEY (category_id) REFERENCES catalog_categories(category_id)
);

CREATE INDEX idx_catalog_products_vendor ON catalog_products(vendor_id);
CREATE INDEX idx_catalog_products_category ON catalog_products(category_id);
CREATE INDEX idx_catalog_products_status ON catalog_products(status);
CREATE INDEX idx_catalog_products_sku ON catalog_products(sku);
CREATE INDEX idx_catalog_products_slug ON catalog_products(slug);
CREATE INDEX idx_catalog_products_in_stock ON catalog_products(in_stock);

-- Full-text search index (PostgreSQL tsvector + GIN)
CREATE INDEX idx_catalog_products_search ON catalog_products USING GIN(search_vector);

-- Trigram index for fuzzy/partial name matching
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX idx_catalog_products_name_trgm ON catalog_products USING GIN(name gin_trgm_ops);

CREATE TABLE product_ratings (
                                 product_id UUID PRIMARY KEY,
                                 average_rating NUMERIC(3, 2) NOT NULL DEFAULT 0.00,
                                 review_count INTEGER NOT NULL DEFAULT 0,
                                 updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

                                 CONSTRAINT fk_product_ratings_product
                                     FOREIGN KEY (product_id) REFERENCES catalog_products(product_id)
                                         ON DELETE CASCADE
);

-- Trigger function: auto-update search_vector whenever name/description/brand changes
CREATE OR REPLACE FUNCTION catalog_products_search_vector_update() RETURNS trigger AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('english', COALESCE(NEW.name, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.brand, '')), 'B') ||
        setweight(to_tsvector('english', COALESCE(NEW.description, '')), 'C');
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_catalog_products_search_vector
    BEFORE INSERT OR UPDATE ON catalog_products
                         FOR EACH ROW
                         EXECUTE FUNCTION catalog_products_search_vector_update();