@echo off
REM Set environment variables for RMS Application
REM Run this script before starting the application

set APP_PORT=8082
set CONSUL_ENABLED=true
set CONSUL_HOST=localhost
set CONSUL_PORT=8500
set CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:4200,http://localhost:8100,http://localhost:9000
set DB_HOST=localhost
set DB_NAME=rms
set DB_PASSWORD=rms
set DB_PORT=5432
set DB_USERNAME=rms
set ELASTICSEARCH_ENABLED=false
set ELASTICSEARCH_HOST=localhost
set ELASTICSEARCH_PORT=9200
set KEYCLOAK_CLIENT_ID=gateway
set KEYCLOAK_CLIENT_SECRET=M5nP8qR2sT6uV9wX1yZ3aC4dE7fG0h
set KEYCLOAK_HOST=rmsauth.atparui.com
set KEYCLOAK_REALM=gateway
set LOG_LEVEL_APP=DEBUG
set LOG_LEVEL_ROOT=INFO
set MULTITENANCY_DEFAULT_TENANT=gateway
set MULTITENANCY_ENABLED=true
set MULTITENANCY_TENANT_HEADER=X-Tenant-ID
set SPRING_DOCKER_COMPOSE_ENABLED=false
set SPRING_PROFILES_ACTIVE=dev

echo Environment variables set successfully!
echo.
echo To start the application, run:
echo   mvnw spring-boot:run
echo.
echo Or use the debugger in Cursor (F5)

