ALTER TABLE orders
    ADD COLUMN priority VARCHAR(16) NOT NULL DEFAULT 'NORMAL';

CREATE INDEX idx_deliveries_date ON deliveries(scheduled_date);