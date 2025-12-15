-- Add stock and version columns to products table for inventory control with optimistic locking

ALTER TABLE products ADD COLUMN stock INT NOT NULL DEFAULT 0;
ALTER TABLE products ADD COLUMN version INT NOT NULL DEFAULT 1;

-- Add check constraint to prevent negative stock
ALTER TABLE products ADD CONSTRAINT chk_stock_non_negative CHECK (stock >= 0);

-- Create index for faster stock queries
CREATE INDEX idx_products_stock ON products(stock) WHERE stock > 0;

-- Add some initial stock to existing products (if any)
UPDATE products SET stock = 10 WHERE stock = 0;
