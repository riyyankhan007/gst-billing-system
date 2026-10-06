-- =============================================================================
-- GST BILLING SYSTEM - FLYWAY MIGRATION V2 (CREDIT & DEBIT NOTES)
-- =============================================================================

-- 1. Credit Note
CREATE TABLE IF NOT EXISTS credit_note (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL REFERENCES business(id) ON DELETE CASCADE,
    invoice_id BIGINT NOT NULL REFERENCES invoice(id) ON DELETE CASCADE,
    customer_id BIGINT NOT NULL REFERENCES customer(id) ON DELETE RESTRICT,
    credit_note_number VARCHAR(100) NOT NULL,
    note_date DATE NOT NULL,
    reason VARCHAR(255) NOT NULL,
    taxable_amount NUMERIC(15, 2) NOT NULL DEFAULT 0,
    cgst NUMERIC(15, 2) NOT NULL DEFAULT 0,
    sgst NUMERIC(15, 2) NOT NULL DEFAULT 0,
    igst NUMERIC(15, 2) NOT NULL DEFAULT 0,
    total_tax NUMERIC(15, 2) NOT NULL DEFAULT 0,
    grand_total NUMERIC(15, 2) NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'ISSUED',
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_business_credit_note_num UNIQUE (business_id, credit_note_number)
);

CREATE INDEX IF NOT EXISTS idx_cn_business ON credit_note(business_id);
CREATE INDEX IF NOT EXISTS idx_cn_invoice ON credit_note(invoice_id);
CREATE INDEX IF NOT EXISTS idx_cn_customer ON credit_note(customer_id);

-- 2. Debit Note
CREATE TABLE IF NOT EXISTS debit_note (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL REFERENCES business(id) ON DELETE CASCADE,
    invoice_id BIGINT REFERENCES invoice(id) ON DELETE CASCADE,
    customer_id BIGINT REFERENCES customer(id) ON DELETE RESTRICT,
    debit_note_number VARCHAR(100) NOT NULL,
    note_date DATE NOT NULL,
    reason VARCHAR(255) NOT NULL,
    taxable_amount NUMERIC(15, 2) NOT NULL DEFAULT 0,
    cgst NUMERIC(15, 2) NOT NULL DEFAULT 0,
    sgst NUMERIC(15, 2) NOT NULL DEFAULT 0,
    igst NUMERIC(15, 2) NOT NULL DEFAULT 0,
    total_tax NUMERIC(15, 2) NOT NULL DEFAULT 0,
    grand_total NUMERIC(15, 2) NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'ISSUED',
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_business_debit_note_num UNIQUE (business_id, debit_note_number)
);

CREATE INDEX IF NOT EXISTS idx_dn_business ON debit_note(business_id);
CREATE INDEX IF NOT EXISTS idx_dn_invoice ON debit_note(invoice_id);
CREATE INDEX IF NOT EXISTS idx_dn_customer ON debit_note(customer_id);
