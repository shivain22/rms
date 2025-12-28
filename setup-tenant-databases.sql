-- Multi-tenant database setup script
-- Run this script in your PostgreSQL instance to create tenant databases

-- Create main application database (if not exists)
CREATE DATABASE rms;

-- Create Keycloak database
CREATE DATABASE keycloak;

-- Create tenant-specific databases
CREATE DATABASE rms_tenant1;
CREATE DATABASE rms_tenant2;

-- Grant permissions to rms user (adjust username as needed)
GRANT ALL PRIVILEGES ON DATABASE rms TO rms;
GRANT ALL PRIVILEGES ON DATABASE keycloak TO rms;
GRANT ALL PRIVILEGES ON DATABASE rms_tenant1 TO rms;
GRANT ALL PRIVILEGES ON DATABASE rms_tenant2 TO rms;

-- Connect to each database and create schema if needed
\c rms;
-- Main database schema will be created by Liquibase

\c rms_tenant1;
-- Tenant1 database schema will be created by Liquibase

\c rms_tenant2;
-- Tenant2 database schema will be created by Liquibase