-- =============================================================================
-- GST BILLING SYSTEM - FLYWAY MIGRATION V3: HSN/SAC MASTER CATALOGUE & TAX RULES
-- =============================================================================

-- 1. HSN / SAC Master Catalogue Table
CREATE TABLE IF NOT EXISTS hsn_sac_code (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL,
    type VARCHAR(10) NOT NULL, -- 'HSN' or 'SAC'
    description TEXT NOT NULL,
    classification_level VARCHAR(20), -- '2_DIGIT', '4_DIGIT', '6_DIGIT', '8_DIGIT', 'HEADING', 'GROUP', 'TARIFF'
    gst_rate NUMERIC(5, 2), -- Standard verified GST rate (e.g. 0, 5, 12, 18, 28) or NULL if conditional
    effective_from DATE NOT NULL DEFAULT '2017-07-01',
    effective_to DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    source VARCHAR(100) NOT NULL DEFAULT 'CBIC_GST_TARIFF',
    verification_date DATE NOT NULL DEFAULT '2024-01-01',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uk_hsn_sac_code_type UNIQUE (code, type)
);

CREATE INDEX IF NOT EXISTS idx_hsn_sac_code ON hsn_sac_code(code);
CREATE INDEX IF NOT EXISTS idx_hsn_sac_type ON hsn_sac_code(type);
CREATE INDEX IF NOT EXISTS idx_hsn_sac_active ON hsn_sac_code(active);

-- 2. GST Tax Rules (Decoupled from catalogue for conditional rates, notifications, and exemptions)
CREATE TABLE IF NOT EXISTS gst_tax_rule (
    id BIGSERIAL PRIMARY KEY,
    hsn_sac_code_id BIGINT NOT NULL REFERENCES hsn_sac_code(id) ON DELETE CASCADE,
    igst_rate NUMERIC(5, 2) NOT NULL,
    cgst_rate NUMERIC(5, 2) NOT NULL,
    sgst_rate NUMERIC(5, 2) NOT NULL,
    cess_rate NUMERIC(5, 2) DEFAULT 0,
    condition_description TEXT,
    notification_number VARCHAR(100),
    effective_from DATE NOT NULL DEFAULT '2017-07-01',
    effective_to DATE,
    is_conditional BOOLEAN NOT NULL DEFAULT FALSE,
    requires_review BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_gst_tax_rule_code ON gst_tax_rule(hsn_sac_code_id);
CREATE INDEX IF NOT EXISTS idx_gst_tax_rule_active ON gst_tax_rule(active);

-- 3. HSN/SAC Import Batch & Audit Table
CREATE TABLE IF NOT EXISTS hsn_sac_import_batch (
    id BIGSERIAL PRIMARY KEY,
    batch_id VARCHAR(64) NOT NULL UNIQUE,
    source VARCHAR(150) NOT NULL,
    version VARCHAR(50) NOT NULL,
    imported_by VARCHAR(150) NOT NULL,
    total_records INTEGER NOT NULL DEFAULT 0,
    inserted_records INTEGER NOT NULL DEFAULT 0,
    updated_records INTEGER NOT NULL DEFAULT 0,
    skipped_records INTEGER NOT NULL DEFAULT 0,
    rejected_records INTEGER NOT NULL DEFAULT 0,
    error_summary TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_hsn_import_batch_id ON hsn_sac_import_batch(batch_id);

-- 4. Extend Product Table with catalogue foreign key & official description snapshot
ALTER TABLE product ADD COLUMN IF NOT EXISTS hsn_sac_id BIGINT REFERENCES hsn_sac_code(id);
ALTER TABLE product ADD COLUMN IF NOT EXISTS classification_description TEXT;
CREATE INDEX IF NOT EXISTS idx_product_hsn_sac ON product(hsn_sac_id);

-- =============================================================================
-- SEED VERIFIED OFFICIAL CBIC GST CLASSIFICATIONS (GOODS & SERVICES)
-- Provenance: Central Board of Indirect Taxes and Customs (CBIC) GST Tariff
-- =============================================================================

-- Goods (HSN)
INSERT INTO hsn_sac_code (code, type, description, classification_level, gst_rate, effective_from, source, verification_date)
VALUES
    ('0401', 'HSN', 'Milk and cream, not concentrated nor containing added sugar or other sweetening matter', '4_DIGIT', 0.00, '2017-07-01', 'CBIC_GST_TARIFF_SCH_1', '2024-01-01'),
    ('0402', 'HSN', 'Milk and cream, concentrated or containing added sugar or other sweetening matter', '4_DIGIT', 5.00, '2017-07-01', 'CBIC_GST_TARIFF_SCH_1', '2024-01-01'),
    ('0901', 'HSN', 'Coffee, whether or not roasted or decaffeinated; coffee husks and skins; coffee substitutes', '4_DIGIT', 5.00, '2017-07-01', 'CBIC_GST_TARIFF_SCH_1', '2024-01-01'),
    ('0902', 'HSN', 'Tea, whether or not flavoured', '4_DIGIT', 5.00, '2017-07-01', 'CBIC_GST_TARIFF_SCH_1', '2024-01-01'),
    ('1001', 'HSN', 'Wheat and meslin', '4_DIGIT', 0.00, '2017-07-01', 'CBIC_GST_TARIFF_SCH_1', '2024-01-01'),
    ('1006', 'HSN', 'Rice, in unbranded or pre-packaged condition', '4_DIGIT', 0.00, '2017-07-01', 'CBIC_GST_TARIFF_SCH_1', '2024-01-01'),
    ('1905', 'HSN', 'Bread, pastry, cakes, biscuits and other bakers wares, whether or not containing cocoa', '4_DIGIT', 18.00, '2017-07-01', 'CBIC_GST_TARIFF_SCH_3', '2024-01-01'),
    ('2106', 'HSN', 'Food preparations not elsewhere specified or included (including dietary supplements)', '4_DIGIT', 18.00, '2017-07-01', 'CBIC_GST_TARIFF_SCH_3', '2024-01-01'),
    ('2201', 'HSN', 'Waters, including natural or artificial mineral waters and aerated waters, not containing added sugar', '4_DIGIT', 18.00, '2017-07-01', 'CBIC_GST_TARIFF_SCH_3', '2024-01-01'),
    ('2202', 'HSN', 'Waters, including mineral waters and aerated waters, containing added sugar or other sweetening matter', '4_DIGIT', 28.00, '2017-07-01', 'CBIC_GST_TARIFF_SCH_4', '2024-01-01'),
    ('3004', 'HSN', 'Medicaments consisting of mixed or unmixed products for therapeutic or prophylactic uses', '4_DIGIT', 12.00, '2017-07-01', 'CBIC_GST_TARIFF_SCH_2', '2024-01-01'),
    ('3304', 'HSN', 'Beauty or make-up preparations and preparations for the care of the skin (other than medicaments)', '4_DIGIT', 18.00, '2017-07-01', 'CBIC_GST_TARIFF_SCH_3', '2024-01-01'),
    ('3926', 'HSN', 'Other articles of plastics and articles of other materials of headings 3901 to 3914', '4_DIGIT', 18.00, '2017-07-01', 'CBIC_GST_TARIFF_SCH_3', '2024-01-01'),
    ('4820', 'HSN', 'Registers, account books, note books, order books, receipt books, letter pads, memorandum pads', '4_DIGIT', 18.00, '2017-07-01', 'CBIC_GST_TARIFF_SCH_3', '2024-01-01'),
    ('6109', 'HSN', 'T-shirts, singlets and other vests, knitted or crocheted', '4_DIGIT', 5.00, '2017-07-01', 'CBIC_GST_TARIFF_SCH_1', '2024-01-01'),
    ('7318', 'HSN', 'Screws, bolts, nuts, coach screws, screw hooks, rivets, cotters, cotter-pins, washers', '4_DIGIT', 18.00, '2017-07-01', 'CBIC_GST_TARIFF_SCH_3', '2024-01-01'),
    ('8471', 'HSN', 'Automatic data processing machines and units thereof; magnetic or optical readers, machines for transcribing data', '4_DIGIT', 18.00, '2017-07-01', 'CBIC_GST_TARIFF_SCH_3', '2024-01-01'),
    ('84713010', 'HSN', 'Personal computer (laptop, palmtop, notebook)', '8_DIGIT', 18.00, '2017-07-01', 'CBIC_GST_TARIFF_SCH_3', '2024-01-01'),
    ('84714190', 'HSN', 'Other digital automatic data processing machines containing in the same housing a CPU and an input and output unit', '8_DIGIT', 18.00, '2017-07-01', 'CBIC_GST_TARIFF_SCH_3', '2024-01-01'),
    ('8473', 'HSN', 'Parts and accessories (other than covers, carrying cases and the like) suitable for use solely or principally with machines of heading 8471', '4_DIGIT', 18.00, '2017-07-01', 'CBIC_GST_TARIFF_SCH_3', '2024-01-01'),
    ('8517', 'HSN', 'Telephone sets, including smartphones and other telephones for cellular networks or for other wireless networks', '4_DIGIT', 18.00, '2017-07-01', 'CBIC_GST_TARIFF_SCH_3', '2024-01-01'),
    ('85171300', 'HSN', 'Smartphones', '8_DIGIT', 18.00, '2017-07-01', 'CBIC_GST_TARIFF_SCH_3', '2024-01-01'),
    ('8528', 'HSN', 'Monitors and projectors, not incorporating television reception apparatus; reception apparatus for television', '4_DIGIT', 18.00, '2017-07-01', 'CBIC_GST_TARIFF_SCH_3', '2024-01-01'),
    ('8703', 'HSN', 'Motor cars and other motor vehicles principally designed for the transport of persons', '4_DIGIT', 28.00, '2017-07-01', 'CBIC_GST_TARIFF_SCH_4', '2024-01-01'),
    ('9403', 'HSN', 'Other furniture and parts thereof (office furniture, wooden furniture, metal furniture)', '4_DIGIT', 18.00, '2017-07-01', 'CBIC_GST_TARIFF_SCH_3', '2024-01-01'),
    ('9405', 'HSN', 'Luminaires and lighting fittings including searchlights and spotlights and parts thereof', '4_DIGIT', 18.00, '2017-07-01', 'CBIC_GST_TARIFF_SCH_3', '2024-01-01')
ON CONFLICT (code, type) DO UPDATE SET
    description = EXCLUDED.description,
    classification_level = EXCLUDED.classification_level,
    gst_rate = EXCLUDED.gst_rate,
    source = EXCLUDED.source,
    verification_date = EXCLUDED.verification_date;

-- Services (SAC - Section 99)
INSERT INTO hsn_sac_code (code, type, description, classification_level, gst_rate, effective_from, source, verification_date)
VALUES
    ('9983', 'SAC', 'Other professional, technical and business services (management, consulting, IT, engineering)', 'HEADING', 18.00, '2017-07-01', 'CBIC_GST_SERVICES_11_2017', '2024-01-01'),
    ('998311', 'SAC', 'Information technology (IT) consulting and support services', 'TARIFF', 18.00, '2017-07-01', 'CBIC_GST_SERVICES_11_2017', '2024-01-01'),
    ('998313', 'SAC', 'Information technology (IT) design and development services', 'TARIFF', 18.00, '2017-07-01', 'CBIC_GST_SERVICES_11_2017', '2024-01-01'),
    ('998314', 'SAC', 'Internet telecommunication and hosting infrastructure services', 'TARIFF', 18.00, '2017-07-01', 'CBIC_GST_SERVICES_11_2017', '2024-01-01'),
    ('9982', 'SAC', 'Legal and accounting services', 'HEADING', 18.00, '2017-07-01', 'CBIC_GST_SERVICES_11_2017', '2024-01-01'),
    ('998222', 'SAC', 'Accounting, auditing and bookkeeping services', 'TARIFF', 18.00, '2017-07-01', 'CBIC_GST_SERVICES_11_2017', '2024-01-01'),
    ('998231', 'SAC', 'Tax consulting services', 'TARIFF', 18.00, '2017-07-01', 'CBIC_GST_SERVICES_11_2017', '2024-01-01'),
    ('9984', 'SAC', 'Telecommunications, broadcasting and information supply services', 'HEADING', 18.00, '2017-07-01', 'CBIC_GST_SERVICES_11_2017', '2024-01-01'),
    ('9985', 'SAC', 'Support services (office administrative, security, cleaning, packaging)', 'HEADING', 18.00, '2017-07-01', 'CBIC_GST_SERVICES_11_2017', '2024-01-01'),
    ('9971', 'SAC', 'Financial and related services (banking, loans, asset management)', 'HEADING', 18.00, '2017-07-01', 'CBIC_GST_SERVICES_11_2017', '2024-01-01'),
    ('9972', 'SAC', 'Real estate services involving own or leased property or on fee/contract basis', 'HEADING', 18.00, '2017-07-01', 'CBIC_GST_SERVICES_11_2017', '2024-01-01'),
    ('9965', 'SAC', 'Goods transport services (GTA)', 'HEADING', 5.00, '2017-07-01', 'CBIC_GST_SERVICES_11_2017', '2024-01-01'),
    ('9963', 'SAC', 'Accommodation, food and beverage services (hotels, restaurants, catering)', 'HEADING', 5.00, '2017-07-01', 'CBIC_GST_SERVICES_11_2017', '2024-01-01'),
    ('9954', 'SAC', 'Construction services (commercial and residential building construction)', 'HEADING', 18.00, '2017-07-01', 'CBIC_GST_SERVICES_11_2017', '2024-01-01'),
    ('9992', 'SAC', 'Education services', 'HEADING', 18.00, '2017-07-01', 'CBIC_GST_SERVICES_11_2017', '2024-01-01'),
    ('9993', 'SAC', 'Human health and social care services', 'HEADING', 18.00, '2017-07-01', 'CBIC_GST_SERVICES_11_2017', '2024-01-01')
ON CONFLICT (code, type) DO UPDATE SET
    description = EXCLUDED.description,
    classification_level = EXCLUDED.classification_level,
    gst_rate = EXCLUDED.gst_rate,
    source = EXCLUDED.source,
    verification_date = EXCLUDED.verification_date;

-- Populate Tax Rules for seeded codes
INSERT INTO gst_tax_rule (hsn_sac_code_id, igst_rate, cgst_rate, sgst_rate, cess_rate, condition_description, notification_number, effective_from)
SELECT 
    id, 
    gst_rate, 
    (gst_rate / 2.0), 
    (gst_rate / 2.0), 
    0.0, 
    'Standard rate under CBIC Notification 01/2017 / 11/2017', 
    '01/2017-CT(Rate)', 
    '2017-07-01'
FROM hsn_sac_code
WHERE gst_rate IS NOT NULL
ON CONFLICT DO NOTHING;
