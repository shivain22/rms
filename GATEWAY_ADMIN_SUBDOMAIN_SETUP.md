# Gateway Admin and Subdomain Configuration

## Overview

This document describes the changes made to support:

1. A new `gwadmin` realm for gateway management
2. Subdomain field in tenant configuration

## Changes Made

### 1. Database Schema Updates

#### New Subdomain Column

- Added `subdomain` column to the `tenants` table
- Updated `create-tenants-table.sql` to include the new column
- Created `add-subdomain-column.sql` migration script for existing installations

#### Updated Tenant Entity

- Added `subdomain` field to `Tenant.java` domain class
- Updated constructor and getter/setter methods
- Added validation annotations

### 2. Gateway Admin Realm Configuration

#### OAuth Configuration

- Added `gwadmin` provider and registration in `application.yml`
- Added `gwadmin` provider and registration in `application-dev.yml`
- Client ID: `gwadmin`
- Client Secret: `gwadmin`
- Issuer URI: `https://rmsauth.atparui.com/realms/gwadmin`

#### New Configuration Class

- Created `GatewayAdminConfig.java` for gateway admin settings
- Configurable via `gateway.admin.*` properties

### 3. Tenant Resolution Updates

#### Enhanced TenantResolver

- Updated to support database-based subdomain lookup
- Added support for gateway admin realm detection
- Improved subdomain extraction logic
- Added support for both `.yourdomain.com` and `.atparui.com` domains

#### New Repository Method

- Added `findBySubdomainAndActiveTrue()` method to `TenantRepository`
- Added `findBySubdomain()` method to `TenantService`

## Usage

### Gateway Admin Access

Access the gateway admin interface using domains containing:

- `admin.` prefix (e.g., `admin.yourdomain.com`)
- `gateway.` prefix (e.g., `gateway.yourdomain.com`)

These will automatically use the `gwadmin` realm with credentials:

- Client ID: `gwadmin`
- Client Secret: `gwadmin`

### Tenant Subdomain Configuration

1. Add tenants to the database with subdomain values
2. Access tenant-specific interfaces using: `{subdomain}.yourdomain.com`
3. The system will automatically resolve the tenant based on the subdomain

### Database Migration

For existing installations, run the migration script:

```sql
-- Run this script to add subdomain column to existing tenants table
\i add-subdomain-column.sql
```

## Configuration Properties

### Gateway Admin (Optional)

```yaml
gateway:
  admin:
    realm: gwadmin
    client-id: gwadmin
    client-secret: gwadmin
    issuer-uri: https://rmsauth.atparui.com/realms/gwadmin
```

### Example Tenant with Subdomain

```sql
INSERT INTO tenants (tenant_id, name, subdomain, database_url, database_username, database_password, schema_name, active)
VALUES ('pizzahut', 'Pizza Hut', 'pizzahut', 'jdbc:postgresql://localhost:5432/pizzahut_db', 'pizzahut_user', 'pizzahut_pass', 'pizzahut_schema', true);
```

## Testing

1. Ensure Keycloak has the `gwadmin` realm configured
2. Test gateway admin access via admin subdomain
3. Test tenant access via configured subdomains
4. Verify database tenant resolution works correctly
