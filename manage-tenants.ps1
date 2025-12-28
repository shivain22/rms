# Multi-tenant database management script for Windows
# Run this script to set up or manage tenant databases

param(
    [Parameter(Mandatory=$false)]
    [string]$Action = "setup",
    
    [Parameter(Mandatory=$false)]
    [string]$TenantName = ""
)

$PostgresHost = "localhost"
$PostgresPort = "5432"
$PostgresUser = "rms"
$PostgresPassword = ""

function Setup-InitialDatabases {
    Write-Host "Setting up initial databases..." -ForegroundColor Green
    
    # Execute the SQL setup script
    if (Test-Path "setup-tenant-databases.sql") {
        psql -h $PostgresHost -p $PostgresPort -U $PostgresUser -f setup-tenant-databases.sql
        Write-Host "Initial databases created successfully!" -ForegroundColor Green
    } else {
        Write-Host "setup-tenant-databases.sql not found!" -ForegroundColor Red
    }
}

function Create-TenantDatabase {
    param([string]$TenantKey)
    
    if ([string]::IsNullOrEmpty($TenantKey)) {
        Write-Host "Please provide a tenant name!" -ForegroundColor Red
        return
    }
    
    $DatabaseName = "rms_$TenantKey"
    Write-Host "Creating database for tenant: $TenantKey" -ForegroundColor Green
    
    # Create database
    psql -h $PostgresHost -p $PostgresPort -U $PostgresUser -c "CREATE DATABASE $DatabaseName;"
    psql -h $PostgresHost -p $PostgresPort -U $PostgresUser -c "GRANT ALL PRIVILEGES ON DATABASE $DatabaseName TO $PostgresUser;"
    
    Write-Host "Database $DatabaseName created successfully!" -ForegroundColor Green
    Write-Host "Don't forget to:" -ForegroundColor Yellow
    Write-Host "1. Add the tenant configuration to MultiTenantDatabaseConfig.java" -ForegroundColor Yellow
    Write-Host "2. Run Liquibase migration on the new database" -ForegroundColor Yellow
}

function Run-LiquibaseMigration {
    param([string]$TenantKey)
    
    if ([string]::IsNullOrEmpty($TenantKey)) {
        $DatabaseName = "rms"
    } else {
        $DatabaseName = "rms_$TenantKey"
    }
    
    Write-Host "Running Liquibase migration for database: $DatabaseName" -ForegroundColor Green
    
    # Run Maven Liquibase update
    mvn liquibase:update -Dliquibase.url="jdbc:postgresql://$PostgresHost`:$PostgresPort/$DatabaseName" -Dliquibase.username=$PostgresUser -Dliquibase.password=$PostgresPassword
}

switch ($Action.ToLower()) {
    "setup" {
        Setup-InitialDatabases
        Write-Host "Running Liquibase migration for main database..." -ForegroundColor Green
        Run-LiquibaseMigration
        Write-Host "Running Liquibase migration for tenant1..." -ForegroundColor Green
        Run-LiquibaseMigration -TenantKey "tenant1"
        Write-Host "Running Liquibase migration for tenant2..." -ForegroundColor Green
        Run-LiquibaseMigration -TenantKey "tenant2"
    }
    "create-tenant" {
        Create-TenantDatabase -TenantKey $TenantName
        Run-LiquibaseMigration -TenantKey $TenantName
    }
    "migrate" {
        Run-LiquibaseMigration -TenantKey $TenantName
    }
    default {
        Write-Host "Usage:" -ForegroundColor Yellow
        Write-Host "  .\manage-tenants.ps1 -Action setup                    # Initial setup" -ForegroundColor Yellow
        Write-Host "  .\manage-tenants.ps1 -Action create-tenant -TenantName tenant3  # Create new tenant" -ForegroundColor Yellow
        Write-Host "  .\manage-tenants.ps1 -Action migrate -TenantName tenant1        # Run migration for specific tenant" -ForegroundColor Yellow
    }
}