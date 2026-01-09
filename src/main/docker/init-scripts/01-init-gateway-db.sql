-- PostgreSQL Initialization Script for RMS Platform
-- This script runs on first container startup (in /docker-entrypoint-initdb.d/)

-- Create the gateway database if it doesn't exist (usually created by POSTGRES_DB env var)
-- This is idempotent - won't fail if database exists
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'rms_gateway') THEN
        CREATE DATABASE rms_gateway WITH ENCODING = 'UTF8';
        RAISE NOTICE 'Created database: rms_gateway';
    ELSE
        RAISE NOTICE 'Database rms_gateway already exists';
    END IF;
END
$$;

-- Grant privileges on gateway database
GRANT ALL PRIVILEGES ON DATABASE rms_gateway TO postgres;

-- Log completion
DO $$
BEGIN
    RAISE NOTICE '===========================================';
    RAISE NOTICE 'PostgreSQL initialization completed!';
    RAISE NOTICE 'Gateway database: rms_gateway';
    RAISE NOTICE 'Admin user: postgres';
    RAISE NOTICE '===========================================';
    RAISE NOTICE '';
    RAISE NOTICE 'Platform databases will be created by the';
    RAISE NOTICE 'PlatformDatabaseInitializer on app startup.';
    RAISE NOTICE '';
    RAISE NOTICE 'The following databases will be created:';
    RAISE NOTICE '  - rms_template, rms_default (Restaurant)';
    RAISE NOTICE '  - ecm_template, ecm_default (E-commerce)';
    RAISE NOTICE '  - nbk_template, nbk_default (Neo Banking)';
    RAISE NOTICE '  - awd_template, awd_default (AWD Farming)';
    RAISE NOTICE '  - vpm_template, vpm_default (Visitor Pass)';
    RAISE NOTICE '  - hms_template, hms_default (Hospital)';
    RAISE NOTICE '  - ems_template, ems_default (Event Mgmt)';
    RAISE NOTICE '  - dms_template, dms_default (Dairy Mgmt)';
    RAISE NOTICE '===========================================';
END
$$;
