CREATE EXTENSION IF NOT EXISTS vector;

ALTER TABLE products ADD COLUMN IF NOT EXISTS is_sale BOOLEAN;
ALTER TABLE products ADD COLUMN IF NOT EXISTS sale_end_time TIMESTAMP WITHOUT TIME ZONE;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'products'
          AND column_name = 'sale'
    ) THEN
        EXECUTE 'UPDATE products SET is_sale = sale WHERE is_sale IS NULL AND sale IS NOT NULL';
    END IF;
END
$$;

CREATE TABLE product_vector_index_state (
    product_id INTEGER PRIMARY KEY,
    content_hash CHAR(64) NOT NULL,
    indexed_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE product_index_outbox (
    product_id INTEGER PRIMARY KEY,
    requested_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    attempts INTEGER NOT NULL DEFAULT 0,
    available_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    locked_until TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX idx_product_index_outbox_available
    ON product_index_outbox(available_at, requested_at);
