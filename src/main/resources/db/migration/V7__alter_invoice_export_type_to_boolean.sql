-- =============================================================================
-- GST BILLING SYSTEM - FLYWAY MIGRATION V7
-- CONVERT INVOICE EXPORT_TYPE FROM VARCHAR TO BOOLEAN FOR HIBERNATE COMPLIANCE
-- =============================================================================

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'invoice' 
          AND column_name = 'export_type' 
          AND data_type = 'character varying'
    ) THEN
        ALTER TABLE invoice 
        ALTER COLUMN export_type TYPE BOOLEAN 
        USING (
            CASE 
                WHEN export_type IS NULL THEN FALSE
                WHEN LOWER(TRIM(export_type)) IN ('true', 't', '1', 'yes', 'y', 'export', 'wpay', 'wopay') THEN TRUE
                ELSE FALSE
            END
        );
        ALTER TABLE invoice ALTER COLUMN export_type SET DEFAULT FALSE;
    END IF;
END $$;
