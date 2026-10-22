-- =============================================================================
-- GST BILLING SYSTEM - FLYWAY MIGRATION V4
-- MULTI-BUSINESS MEMBERSHIP, MULTI-WAREHOUSE INVENTORY & FINANCIAL HARDENING
-- =============================================================================

-- 1. Business Membership Table
CREATE TABLE IF NOT EXISTS business_membership (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    business_id BIGINT NOT NULL REFERENCES business(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL DEFAULT 'OWNER',
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    invited_by VARCHAR(150),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uk_user_business UNIQUE (user_id, business_id)
);

CREATE INDEX IF NOT EXISTS idx_membership_user ON business_membership(user_id);
CREATE INDEX IF NOT EXISTS idx_membership_business ON business_membership(business_id);
CREATE INDEX IF NOT EXISTS idx_membership_status ON business_membership(status);

-- Backfill business memberships from existing user-business relationships safely
INSERT INTO business_membership (user_id, business_id, role, status, created_at)
SELECT u.id, u.business_id, COALESCE(u.role, 'OWNER'), 'ACTIVE', CURRENT_TIMESTAMP
FROM app_user u
WHERE u.business_id IS NOT NULL
ON CONFLICT (user_id, business_id) DO NOTHING;

-- Make app_user.business_id nullable so users can belong to multiple businesses
ALTER TABLE app_user ALTER COLUMN business_id DROP NOT NULL;


-- 2. Warehouse Table
CREATE TABLE IF NOT EXISTS warehouse (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL REFERENCES business(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL,
    address TEXT,
    state VARCHAR(100),
    state_code VARCHAR(10),
    contact_person VARCHAR(150),
    contact_phone VARCHAR(50),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uk_business_warehouse_code UNIQUE (business_id, code)
);

CREATE INDEX IF NOT EXISTS idx_warehouse_business ON warehouse(business_id);
CREATE INDEX IF NOT EXISTS idx_warehouse_active ON warehouse(active);

-- Backfill a default "Main Warehouse" for every existing business
INSERT INTO warehouse (business_id, name, code, address, state, state_code, is_default, active, created_at)
SELECT b.id, 'Main Warehouse', 'MAIN', b.address, b.state, b.state_code, TRUE, TRUE, CURRENT_TIMESTAMP
FROM business b
ON CONFLICT (business_id, code) DO NOTHING;


-- 3. Warehouse Stock Balance Table
CREATE TABLE IF NOT EXISTS warehouse_stock (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL REFERENCES business(id) ON DELETE CASCADE,
    warehouse_id BIGINT NOT NULL REFERENCES warehouse(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    quantity_on_hand NUMERIC(15, 3) NOT NULL DEFAULT 0,
    quantity_reserved NUMERIC(15, 3) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uk_warehouse_product UNIQUE (warehouse_id, product_id)
);

CREATE INDEX IF NOT EXISTS idx_wh_stock_warehouse ON warehouse_stock(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_wh_stock_product ON warehouse_stock(product_id);
CREATE INDEX IF NOT EXISTS idx_wh_stock_business ON warehouse_stock(business_id);

-- Backfill initial warehouse stock for existing products in their business's default warehouse
INSERT INTO warehouse_stock (business_id, warehouse_id, product_id, quantity_on_hand, quantity_reserved, created_at)
SELECT p.business_id, w.id, p.id, COALESCE(p.stock_quantity, 0), 0, CURRENT_TIMESTAMP
FROM product p
JOIN warehouse w ON w.business_id = p.business_id AND w.is_default = TRUE
ON CONFLICT (warehouse_id, product_id) DO NOTHING;


-- 4. Extend Stock Movement with Warehouse Tracking
ALTER TABLE stock_movement ADD COLUMN IF NOT EXISTS warehouse_id BIGINT REFERENCES warehouse(id) ON DELETE SET NULL;
ALTER TABLE stock_movement ADD COLUMN IF NOT EXISTS target_warehouse_id BIGINT REFERENCES warehouse(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_stock_mov_warehouse ON stock_movement(warehouse_id);

-- Backfill default warehouse for legacy stock movements
UPDATE stock_movement sm
SET warehouse_id = w.id
FROM warehouse w
WHERE sm.warehouse_id IS NULL AND w.business_id = sm.business_id AND w.is_default = TRUE;


-- 5. Extend Invoice with Warehouse Allocation
ALTER TABLE invoice ADD COLUMN IF NOT EXISTS warehouse_id BIGINT REFERENCES warehouse(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_invoice_warehouse ON invoice(warehouse_id);

-- Backfill default warehouse for existing invoices
UPDATE invoice inv
SET warehouse_id = w.id
FROM warehouse w
WHERE inv.warehouse_id IS NULL AND w.business_id = inv.business_id AND w.is_default = TRUE;


-- 6. Credit and Debit Notes Financial & Numbering Hardening
ALTER TABLE credit_note ADD COLUMN IF NOT EXISTS financial_year VARCHAR(10);
ALTER TABLE credit_note ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
ALTER TABLE credit_note ADD COLUMN IF NOT EXISTS created_by VARCHAR(150);

ALTER TABLE debit_note ADD COLUMN IF NOT EXISTS financial_year VARCHAR(10);
ALTER TABLE debit_note ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
ALTER TABLE debit_note ADD COLUMN IF NOT EXISTS created_by VARCHAR(150);

-- Backfill financial year for existing credit/debit notes
UPDATE credit_note
SET financial_year = CASE
    WHEN EXTRACT(MONTH FROM note_date) >= 4 THEN
        EXTRACT(YEAR FROM note_date)::TEXT || '-' || SUBSTRING((EXTRACT(YEAR FROM note_date) + 1)::TEXT FROM 3 FOR 2)
    ELSE
        (EXTRACT(YEAR FROM note_date) - 1)::TEXT || '-' || SUBSTRING(EXTRACT(YEAR FROM note_date)::TEXT FROM 3 FOR 2)
END
WHERE financial_year IS NULL;

UPDATE debit_note
SET financial_year = CASE
    WHEN EXTRACT(MONTH FROM note_date) >= 4 THEN
        EXTRACT(YEAR FROM note_date)::TEXT || '-' || SUBSTRING((EXTRACT(YEAR FROM note_date) + 1)::TEXT FROM 3 FOR 2)
    ELSE
        (EXTRACT(YEAR FROM note_date) - 1)::TEXT || '-' || SUBSTRING(EXTRACT(YEAR FROM note_date)::TEXT FROM 3 FOR 2)
END
WHERE financial_year IS NULL;


-- 7. Payment Webhook Idempotency & Duplicate Prevention
-- Guarantee no duplicate settlement for same gateway payment id per business
CREATE UNIQUE INDEX IF NOT EXISTS uk_payment_business_gateway_id
ON payment(business_id, gateway_payment_id)
WHERE gateway_payment_id IS NOT NULL;
