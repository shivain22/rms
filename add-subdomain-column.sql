-- Migration script to add subdomain column to existing tenants table
-- Run this script if you already have a tenants table without the subdomain column

-- Add subdomain column if it doesn't exist
DO $$ 
BEGIN 
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'tenants' AND column_name = 'subdomain'
    ) THEN
        ALTER TABLE tenants ADD COLUMN subdomain VARCHAR(100);
    END IF;
END $$;

-- Update existing records with default subdomain values (optional)
-- You can customize these based on your existing tenant data
UPDATE tenants SET subdomain = tenant_id WHERE subdomain IS NULL;

-- Create index on subdomain for faster lookups
CREATE INDEX IF NOT EXISTS idx_tenants_subdomain ON tenants(subdomain);