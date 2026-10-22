-- =============================================================================
-- GST BILLING SYSTEM - FLYWAY MIGRATION V5
-- SYNC MISSING DEFAULT WAREHOUSES AND WAREHOUSE STOCK BALANCES
-- =============================================================================

-- 1. Ensure a default warehouse exists for every registered business
INSERT INTO warehouse (business_id, name, code, is_default, active, created_at)
SELECT b.id, 'Main Warehouse', 'MAIN', TRUE, TRUE, CURRENT_TIMESTAMP
FROM business b
WHERE NOT EXISTS (
    SELECT 1 FROM warehouse w WHERE w.business_id = b.id AND w.is_default = TRUE
)
ON CONFLICT (business_id, code) DO NOTHING;

-- 2. Backfill warehouse stock for any physical products missing a record in their business default warehouse
INSERT INTO warehouse_stock (business_id, warehouse_id, product_id, quantity_on_hand, quantity_reserved, created_at)
SELECT p.business_id, w.id, p.id, COALESCE(p.stock_quantity, 0), 0, CURRENT_TIMESTAMP
FROM product p
JOIN warehouse w ON w.business_id = p.business_id AND w.is_default = TRUE
WHERE (p.product_type IS NULL OR UPPER(p.product_type) != 'SERVICE')
  AND NOT EXISTS (
    SELECT 1 FROM warehouse_stock ws WHERE ws.warehouse_id = w.id AND ws.product_id = p.id
)
ON CONFLICT (warehouse_id, product_id) DO NOTHING;

-- 3. Backfill warehouse_id for any legacy stock movement records that have null warehouse_id
UPDATE stock_movement sm
SET warehouse_id = w.id
FROM warehouse w
WHERE sm.warehouse_id IS NULL AND w.business_id = sm.business_id AND w.is_default = TRUE;
