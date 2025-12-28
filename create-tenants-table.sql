-- Create tenants table for multi-tenant management
CREATE TABLE IF NOT EXISTS tenants (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    subdomain VARCHAR(100),
    database_url VARCHAR(255) NOT NULL,
    database_username VARCHAR(100) NOT NULL,
    database_password VARCHAR(255) NOT NULL,
    schema_name VARCHAR(100) NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create index on tenant_id for faster lookups
CREATE INDEX IF NOT EXISTS idx_tenants_tenant_id ON tenants(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenants_active ON tenants(active);

-- Insert sample tenant data (optional)
INSERT INTO tenants (tenant_id, name, subdomain, database_url, database_username, database_password, schema_name, active) 
VALUES 
    ('tenant1', 'Tenant One', 'tenant1', 'jdbc:postgresql://localhost:5432/tenant1_db', 'tenant1_user', 'tenant1_pass', 'tenant1_schema', true),
    ('tenant2', 'Tenant Two', 'tenant2', 'jdbc:postgresql://localhost:5432/tenant2_db', 'tenant2_user', 'tenant2_pass', 'tenant2_schema', true)
ON CONFLICT (tenant_id) DO NOTHING;