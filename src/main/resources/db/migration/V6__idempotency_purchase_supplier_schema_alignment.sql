-- =============================================================================
-- GST BILLING SYSTEM - FLYWAY MIGRATION V6
-- SCHEMA ALIGNMENT: API IDEMPOTENCY KEY, PURCHASE FINANCIALS & SUPPLIER AUDIT
-- =============================================================================

-- 1. Align api_idempotency_key with IdempotencyKeyRecord entity
DO $$
BEGIN
    -- Rename request_payload_hash to request_hash if legacy column exists and new one does not
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'api_idempotency_key' AND column_name = 'request_payload_hash'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'api_idempotency_key' AND column_name = 'request_hash'
    ) THEN
        ALTER TABLE api_idempotency_key RENAME COLUMN request_payload_hash TO request_hash;
    ELSIF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'api_idempotency_key' AND column_name = 'request_hash'
    ) THEN
        ALTER TABLE api_idempotency_key ADD COLUMN request_hash VARCHAR(64);
    END IF;
END $$;

-- Ensure request_hash is populated and NOT NULL
UPDATE api_idempotency_key SET request_hash = '' WHERE request_hash IS NULL;
ALTER TABLE api_idempotency_key ALTER COLUMN request_hash SET NOT NULL;

-- Drop NOT NULL constraints from unmapped columns in IdempotencyKeyRecord to prevent insert failures
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'api_idempotency_key' AND column_name = 'method'
    ) THEN
        ALTER TABLE api_idempotency_key ALTER COLUMN method DROP NOT NULL;
        ALTER TABLE api_idempotency_key ALTER COLUMN method SET DEFAULT 'POST';
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'api_idempotency_key' AND column_name = 'request_uri'
    ) THEN
        ALTER TABLE api_idempotency_key ALTER COLUMN request_uri DROP NOT NULL;
        ALTER TABLE api_idempotency_key ALTER COLUMN request_uri SET DEFAULT '';
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'api_idempotency_key' AND column_name = 'request_payload_hash'
    ) THEN
        ALTER TABLE api_idempotency_key ALTER COLUMN request_payload_hash DROP NOT NULL;
    END IF;
END $$;

-- Supporting indexes for idempotency lookups and expiry cleanup
CREATE INDEX IF NOT EXISTS idx_idempotency_key ON api_idempotency_key(idempotency_key);
CREATE INDEX IF NOT EXISTS idx_idempotency_expires ON api_idempotency_key(expires_at);


-- 2. Align purchase with Purchase entity (paid_amount, balance_amount, payment_status)
ALTER TABLE purchase ADD COLUMN IF NOT EXISTS paid_amount NUMERIC(15, 2) NOT NULL DEFAULT 0;
ALTER TABLE purchase ADD COLUMN IF NOT EXISTS balance_amount NUMERIC(15, 2) NOT NULL DEFAULT 0;
ALTER TABLE purchase ADD COLUMN IF NOT EXISTS payment_status VARCHAR(50) NOT NULL DEFAULT 'UNPAID';

-- Backfill balance_amount and payment_status for any existing purchase records
UPDATE purchase
SET balance_amount = GREATEST(grand_total - COALESCE(paid_amount, 0), 0),
    payment_status = CASE
        WHEN COALESCE(paid_amount, 0) >= grand_total AND grand_total > 0 THEN 'PAID'
        WHEN COALESCE(paid_amount, 0) > 0 THEN 'PARTIALLY_PAID'
        ELSE 'UNPAID'
    END
WHERE (balance_amount = 0 AND COALESCE(paid_amount, 0) = 0 AND grand_total > 0)
   OR payment_status IS NULL;


-- 3. Align supplier with Supplier entity (created_at)
ALTER TABLE supplier ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
UPDATE supplier SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL;
