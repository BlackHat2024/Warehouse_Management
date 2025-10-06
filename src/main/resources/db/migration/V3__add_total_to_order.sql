ALTER TABLE orders
  ADD COLUMN total DECIMAL(19,2) DEFAULT 0.00
  AFTER submitted_date;