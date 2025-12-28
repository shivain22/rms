@echo off
echo Setting up RMS Multi-tenant Application...

REM Load environment variables from .env.local
if exist .env.local (
    for /f "usebackq tokens=1,2 delims==" %%a in (".env.local") do (
        if not "%%a"=="" if not "%%a:~0,1%"=="#" set %%a=%%b
    )
    echo Loaded .env.local
) else (
    echo Warning: .env.local not found, using default values
    set DB_HOST=localhost
    set DB_PORT=5432
    set DB_NAME=rms
    set DB_USERNAME=rms
    set DB_PASSWORD=
    set DB_TENANT1_NAME=rms_tenant1
    set DB_TENANT2_NAME=rms_tenant2
)

REM Start local services
echo Starting local PostgreSQL and other services...
docker-compose -f docker-compose-local.yml --env-file .env.local up -d postgresql consul elasticsearch

REM Wait for PostgreSQL to be ready
echo Waiting for PostgreSQL to be ready...
timeout /t 10 /nobreak

REM Run database migrations
echo Running database migrations...
call mvnw.cmd liquibase:update -Dliquibase.url=jdbc:postgresql://%DB_HOST%:%DB_PORT%/%DB_NAME% -Dliquibase.username=%DB_USERNAME% -Dliquibase.password=%DB_PASSWORD%
call mvnw.cmd liquibase:update -Dliquibase.url=jdbc:postgresql://%DB_HOST%:%DB_PORT%/%DB_TENANT1_NAME% -Dliquibase.username=%DB_USERNAME% -Dliquibase.password=%DB_PASSWORD%
call mvnw.cmd liquibase:update -Dliquibase.url=jdbc:postgresql://%DB_HOST%:%DB_PORT%/%DB_TENANT2_NAME% -Dliquibase.username=%DB_USERNAME% -Dliquibase.password=%DB_PASSWORD%

echo Setup complete! You can now run the application in IntelliJ with the environment variables from .env.local
pause