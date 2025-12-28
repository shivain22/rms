# Tenant Management Feature

## Overview

The Tenant Management feature allows platform administrators to configure and manage multiple tenants in the RMS gateway application. This is designed for multi-tenant SaaS scenarios where each tenant has their own database configuration.

## Features

- **Create Tenants**: Add new tenants with their database configurations
- **View Tenants**: List all tenants with their status and details
- **Edit Tenants**: Update tenant information and database settings
- **Activate/Deactivate**: Enable or disable tenants
- **Delete Tenants**: Remove tenants (soft delete - sets active to false)

## Access

- **URL**: `/admin/tenant-management`
- **Permission**: Requires `ROLE_ADMIN` authority
- **Menu**: Administration → Tenant Management

## Setup Instructions

### 1. Database Setup

Run the SQL script to create the tenants table:

```sql
-- Execute the create-tenants-table.sql script
```

### 2. Configuration

Ensure your application has the master database configuration for storing tenant information.

### 3. Usage

1. Login as an admin user
2. Navigate to Administration → Tenant Management
3. Click "Create a new Tenant" to add tenants
4. Fill in the required information:
   - **Tenant ID**: Unique identifier for the tenant
   - **Name**: Display name for the tenant
   - **Database URL**: JDBC URL for tenant's database
   - **Database Username**: Database user for tenant
   - **Database Password**: Database password for tenant
   - **Schema Name**: Database schema name for tenant
   - **Active**: Whether the tenant is active

## Tenant Configuration Fields

| Field             | Description                    | Example                                       |
| ----------------- | ------------------------------ | --------------------------------------------- |
| Tenant ID         | Unique identifier (2-50 chars) | `tenant1`                                     |
| Name              | Display name (2-100 chars)     | `Acme Corporation`                            |
| Database URL      | JDBC connection URL            | `jdbc:postgresql://localhost:5432/tenant1_db` |
| Database Username | DB user for tenant             | `tenant1_user`                                |
| Database Password | DB password for tenant         | `secure_password`                             |
| Schema Name       | Database schema                | `tenant1_schema`                              |
| Active            | Tenant status                  | `true/false`                                  |

## Security

- All tenant management operations require admin privileges
- Database passwords are stored securely
- Only active tenants can be used for connections
- Tenant deletion is soft delete (sets active=false)

## API Endpoints

- `GET /api/tenants` - List all tenants
- `POST /api/tenants` - Create new tenant
- `GET /api/tenants/{id}` - Get tenant by ID
- `PUT /api/tenants/{id}` - Update tenant
- `DELETE /api/tenants/{id}` - Delete tenant (soft delete)

## Integration

The tenant management system integrates with:

- Multi-tenant connection factory for database routing
- Security system for access control
- Audit system for tracking changes

## Troubleshooting

- Ensure the master database is accessible
- Verify admin user has proper roles
- Check database connectivity for tenant configurations
- Review application logs for connection issues
