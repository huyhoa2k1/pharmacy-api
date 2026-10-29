ALTER TABLE orders ADD COLUMN IF NOT EXISTS payos_order_code BIGINT;
CREATE UNIQUE INDEX IF NOT EXISTS uk_orders_payos_order_code ON orders (payos_order_code);
