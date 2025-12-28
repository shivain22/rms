# PowerShell script to set environment variables for RMS Application
# Run this script before starting the application: .\set-env-vars.ps1

$env:APP_PORT = "8082"
$env:CONSUL_ENABLED = "true"
$env:CONSUL_HOST = "localhost"
$env:CONSUL_PORT = "8500"
$env:CORS_ALLOWED_ORIGINS = "http://localhost:3000,http://localhost:4200,http://localhost:8100,http://localhost:9000"
$env:DB_HOST = "localhost"
$env:DB_NAME = "rms"
$env:DB_PASSWORD = "rms"
$env:DB_PORT = "5432"
$env:DB_USERNAME = "rms"
$env:ELASTICSEARCH_ENABLED = "false"
$env:ELASTICSEARCH_HOST = "localhost"
$env:ELASTICSEARCH_PORT = "9200"
$env:KEYCLOAK_CLIENT_ID = "gateway"
$env:KEYCLOAK_CLIENT_SECRET = "M5nP8qR2sT6uV9wX1yZ3aC4dE7fG0h"
$env:KEYCLOAK_HOST = "rmsauth.atparui.com"
$env:KEYCLOAK_REALM = "gateway"
$env:LOG_LEVEL_APP = "DEBUG"
$env:LOG_LEVEL_ROOT = "INFO"
$env:MULTITENANCY_DEFAULT_TENANT = "gateway"
$env:MULTITENANCY_ENABLED = "true"
$env:MULTITENANCY_TENANT_HEADER = "X-Tenant-ID"
$env:SPRING_DOCKER_COMPOSE_ENABLED = "false"
$env:SPRING_PROFILES_ACTIVE = "dev"

Write-Host "Environment variables set successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "To start the application, run:" -ForegroundColor Yellow
Write-Host "  .\mvnw.cmd spring-boot:run" -ForegroundColor Cyan
Write-Host ""
Write-Host "Or use the debugger in Cursor (F5)" -ForegroundColor Yellow

