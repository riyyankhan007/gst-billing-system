-- =============================================================================
-- GST BILLING SYSTEM - FLYWAY BASELINE SCHEMA MIGRATION V1
-- =============================================================================

-- 1. Business (Tenant)
CREATE TABLE IF NOT EXISTS business (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    gstin VARCHAR(20),
    pan VARCHAR(20),
    address TEXT,
    state VARCHAR(100),
    state_code VARCHAR(10),
    phone VARCHAR(50),
    email VARCHAR(100),
    website VARCHAR(150),
    bank_name VARCHAR(100),
    bank_account_number VARCHAR(50),
    bank_ifsc VARCHAR(20),
    upi_id VARCHAR(100),
    upi_qr_code VARCHAR(255),
    invoice_prefix VARCHAR(20) DEFAULT 'INV',
    invoice_seq_number BIGINT DEFAULT 1,
    financial_year VARCHAR(20),
    default_terms TEXT,
    logo_path VARCHAR(255),
    signature_path VARCHAR(255)
);

-- 2. App User
CREATE TABLE IF NOT EXISTS app_user (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL REFERENCES business(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'OWNER',
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMP,
    reset_password_token VARCHAR(255),
    reset_password_expires_at TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_app_user_business ON app_user(business_id);
CREATE INDEX IF NOT EXISTS idx_app_user_email ON app_user(email);

-- 3. Customer
CREATE TABLE IF NOT EXISTS customer (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL REFERENCES business(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(150),
    phone VARCHAR(50),
    gstin VARCHAR(20),
    pan VARCHAR(20),
    customer_type VARCHAR(50),
    billing_address TEXT,
    shipping_address TEXT,
    address TEXT,
    state VARCHAR(100),
    state_code VARCHAR(10),
    credit_limit NUMERIC(15, 2),
    opening_balance NUMERIC(15, 2),
    payment_terms VARCHAR(100)
);
CREATE INDEX IF NOT EXISTS idx_customer_business ON customer(business_id);

-- 4. Supplier
CREATE TABLE IF NOT EXISTS supplier (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL REFERENCES business(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(150),
    phone VARCHAR(50),
    gstin VARCHAR(20),
    pan VARCHAR(20),
    address TEXT,
    state VARCHAR(100),
    state_code VARCHAR(10),
    bank_name VARCHAR(100),
    bank_account_number VARCHAR(50),
    bank_ifsc VARCHAR(20),
    opening_balance NUMERIC(15, 2),
    payment_terms VARCHAR(100)
);
CREATE INDEX IF NOT EXISTS idx_supplier_business ON supplier(business_id);

-- 5. Product
CREATE TABLE IF NOT EXISTS product (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL REFERENCES business(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    sku VARCHAR(100),
    hsn_code VARCHAR(20),
    price NUMERIC(15, 2) NOT NULL,
    gst_rate NUMERIC(5, 2) NOT NULL DEFAULT 0,
    unit VARCHAR(20),
    stock_quantity NUMERIC(15, 3) NOT NULL DEFAULT 0,
    low_stock_threshold NUMERIC(15, 3) DEFAULT 0,
    tax_inclusive BOOLEAN DEFAULT FALSE,
    active BOOLEAN DEFAULT TRUE,
    product_type VARCHAR(50) DEFAULT 'GOODS',
    discount NUMERIC(5, 2) DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_product_business ON product(business_id);

-- 6. Invoice Sequence
CREATE TABLE IF NOT EXISTS invoice_sequence (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL REFERENCES business(id) ON DELETE CASCADE,
    financial_year VARCHAR(10) NOT NULL,
    doc_type VARCHAR(30) NOT NULL DEFAULT 'INVOICE',
    prefix VARCHAR(20) DEFAULT 'INV',
    current_sequence BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP,
    CONSTRAINT uk_business_fy_doc_type UNIQUE (business_id, financial_year, doc_type)
);

-- 7. Invoice
CREATE TABLE IF NOT EXISTS invoice (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL REFERENCES business(id) ON DELETE CASCADE,
    customer_id BIGINT NOT NULL REFERENCES customer(id),
    invoice_number VARCHAR(100) NOT NULL,
    invoice_date DATE NOT NULL,
    due_date DATE,
    status VARCHAR(50) NOT NULL DEFAULT 'ISSUED',
    taxable_amount NUMERIC(15, 2) NOT NULL DEFAULT 0,
    cgst NUMERIC(15, 2) NOT NULL DEFAULT 0,
    sgst NUMERIC(15, 2) NOT NULL DEFAULT 0,
    igst NUMERIC(15, 2) NOT NULL DEFAULT 0,
    total_tax NUMERIC(15, 2) NOT NULL DEFAULT 0,
    discount_amount NUMERIC(15, 2) DEFAULT 0,
    round_off_amount NUMERIC(15, 2) DEFAULT 0,
    grand_total NUMERIC(15, 2) NOT NULL DEFAULT 0,
    paid_amount NUMERIC(15, 2) DEFAULT 0,
    balance_amount NUMERIC(15, 2) DEFAULT 0,
    supplier_state VARCHAR(100),
    customer_state VARCHAR(100),
    reverse_charge BOOLEAN DEFAULT FALSE,
    export_type VARCHAR(50),
    amount_in_words TEXT,
    terms_and_conditions TEXT,
    notes TEXT,
    financial_year VARCHAR(20),
    cancellation_reason TEXT,
    cancelled_at TIMESTAMP,
    cancelled_by VARCHAR(150),
    irn VARCHAR(64),
    ack_no VARCHAR(64),
    ack_date TIMESTAMP,
    signed_qr_code TEXT,
    einvoice_status VARCHAR(30),
    einvoice_error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_invoice_business ON invoice(business_id);
CREATE INDEX IF NOT EXISTS idx_invoice_customer ON invoice(customer_id);
CREATE INDEX IF NOT EXISTS idx_invoice_number ON invoice(invoice_number);

-- 8. Invoice Item
CREATE TABLE IF NOT EXISTS invoice_item (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT NOT NULL REFERENCES invoice(id) ON DELETE CASCADE,
    product_id BIGINT REFERENCES product(id),
    product_name VARCHAR(255),
    hsn_code VARCHAR(20),
    quantity NUMERIC(15, 3) NOT NULL,
    unit VARCHAR(20),
    unit_price NUMERIC(15, 2) NOT NULL,
    discount NUMERIC(5, 2) DEFAULT 0,
    gst_rate NUMERIC(5, 2) NOT NULL DEFAULT 0,
    taxable_amount NUMERIC(15, 2) NOT NULL,
    tax_amount NUMERIC(15, 2) NOT NULL,
    total_amount NUMERIC(15, 2) NOT NULL,
    tax_inclusive BOOLEAN DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_invoice_item_invoice ON invoice_item(invoice_id);

-- 9. Payment
CREATE TABLE IF NOT EXISTS payment (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL REFERENCES business(id) ON DELETE CASCADE,
    invoice_id BIGINT NOT NULL REFERENCES invoice(id) ON DELETE CASCADE,
    customer_id BIGINT NOT NULL REFERENCES customer(id),
    amount NUMERIC(15, 2) NOT NULL,
    payment_date DATE NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    reference_number VARCHAR(100),
    receipt_number VARCHAR(100),
    notes TEXT,
    created_by VARCHAR(150),
    gateway_provider VARCHAR(50),
    gateway_payment_id VARCHAR(100),
    gateway_order_id VARCHAR(100),
    gateway_signature VARCHAR(255),
    status VARCHAR(50) DEFAULT 'SUCCESS',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_payment_business ON payment(business_id);
CREATE INDEX IF NOT EXISTS idx_payment_invoice ON payment(invoice_id);
CREATE INDEX IF NOT EXISTS idx_payment_customer ON payment(customer_id);
CREATE INDEX IF NOT EXISTS idx_payment_receipt_no ON payment(receipt_number);
CREATE INDEX IF NOT EXISTS idx_payment_gateway_id ON payment(gateway_payment_id);

-- 10. Purchase
CREATE TABLE IF NOT EXISTS purchase (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL REFERENCES business(id) ON DELETE CASCADE,
    supplier_id BIGINT NOT NULL REFERENCES supplier(id),
    supplier_invoice_number VARCHAR(100) NOT NULL,
    purchase_date DATE NOT NULL,
    due_date DATE,
    supplier_state VARCHAR(100),
    taxable_amount NUMERIC(15, 2) NOT NULL DEFAULT 0,
    cgst NUMERIC(15, 2) NOT NULL DEFAULT 0,
    sgst NUMERIC(15, 2) NOT NULL DEFAULT 0,
    igst NUMERIC(15, 2) NOT NULL DEFAULT 0,
    total_tax NUMERIC(15, 2) NOT NULL DEFAULT 0,
    grand_total NUMERIC(15, 2) NOT NULL DEFAULT 0,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_purchase_business ON purchase(business_id);
CREATE INDEX IF NOT EXISTS idx_purchase_supplier ON purchase(supplier_id);

-- 11. Purchase Item
CREATE TABLE IF NOT EXISTS purchase_item (
    id BIGSERIAL PRIMARY KEY,
    purchase_id BIGINT NOT NULL REFERENCES purchase(id) ON DELETE CASCADE,
    product_id BIGINT REFERENCES product(id),
    product_name VARCHAR(255),
    hsn_code VARCHAR(20),
    quantity NUMERIC(15, 3) NOT NULL,
    unit VARCHAR(20),
    unit_price NUMERIC(15, 2) NOT NULL,
    discount NUMERIC(5, 2) DEFAULT 0,
    gst_rate NUMERIC(5, 2) NOT NULL DEFAULT 0,
    taxable_amount NUMERIC(15, 2) NOT NULL,
    tax_amount NUMERIC(15, 2) NOT NULL,
    total_amount NUMERIC(15, 2) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_purchase_item_purchase ON purchase_item(purchase_id);

-- 12. Stock Movement
CREATE TABLE IF NOT EXISTS stock_movement (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL REFERENCES business(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES product(id),
    movement_type VARCHAR(50) NOT NULL,
    quantity NUMERIC(15, 3) NOT NULL,
    stock_before NUMERIC(15, 3),
    stock_after NUMERIC(15, 3),
    reference_number VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_stock_mov_business ON stock_movement(business_id);
CREATE INDEX IF NOT EXISTS idx_stock_mov_product ON stock_movement(product_id);

-- 13. E-Way Bill
CREATE TABLE IF NOT EXISTS eway_bill (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL REFERENCES business(id) ON DELETE CASCADE,
    invoice_id BIGINT NOT NULL REFERENCES invoice(id) ON DELETE CASCADE,
    ewb_number VARCHAR(20) NOT NULL,
    ewb_date TIMESTAMP NOT NULL,
    valid_upto TIMESTAMP NOT NULL,
    distance_km INTEGER NOT NULL,
    vehicle_number VARCHAR(30),
    vehicle_type VARCHAR(20) DEFAULT 'REGULAR',
    transport_mode VARCHAR(20) DEFAULT 'ROAD',
    transporter_id VARCHAR(50),
    transporter_name VARCHAR(150),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    cancel_reason VARCHAR(255),
    cancelled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ewb_business ON eway_bill(business_id);
CREATE INDEX IF NOT EXISTS idx_ewb_invoice ON eway_bill(invoice_id);
CREATE INDEX IF NOT EXISTS idx_ewb_number ON eway_bill(ewb_number);


-- 14. Audit Log
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL REFERENCES business(id) ON DELETE CASCADE,
    actor_user_id BIGINT,
    user_email VARCHAR(150),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT,
    details TEXT,
    ip_address VARCHAR(50),
    user_agent TEXT,
    correlation_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_audit_log_business ON audit_log(business_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_action ON audit_log(action);
CREATE INDEX IF NOT EXISTS idx_audit_log_correlation ON audit_log(correlation_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON audit_log(created_at);

-- 15. API Idempotency Key
CREATE TABLE IF NOT EXISTS api_idempotency_key (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(128) NOT NULL,
    business_id BIGINT NOT NULL REFERENCES business(id) ON DELETE CASCADE,
    user_email VARCHAR(150),
    method VARCHAR(10) NOT NULL,
    request_uri VARCHAR(500) NOT NULL,
    request_payload_hash VARCHAR(64) NOT NULL,
    response_status INTEGER,
    response_content_type VARCHAR(100),
    response_body TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_idempotency_key_tenant UNIQUE (idempotency_key, business_id)
);
CREATE INDEX IF NOT EXISTS idx_idempotency_key_lookup ON api_idempotency_key(idempotency_key, business_id);
CREATE INDEX IF NOT EXISTS idx_idempotency_key_expires ON api_idempotency_key(expires_at);
