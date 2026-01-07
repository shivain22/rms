#!/bin/bash

echo "The application will start in ${JHIPSTER_SLEEP}s..." && sleep ${JHIPSTER_SLEEP}

# usage: file_env VAR [DEFAULT]
#    ie: file_env 'XYZ_DB_PASSWORD' 'example'
# (will allow for "$XYZ_DB_PASSWORD_FILE" to fill in the value of
#  "$XYZ_DB_PASSWORD" from a file, especially for Docker's secrets feature)
file_env() {
    local var="$1"
    local fileVar="${var}_FILE"
    local def="${2:-}"
    if [[ ${!var:-} && ${!fileVar:-} ]]; then
        echo >&2 "error: both $var and $fileVar are set (but are exclusive)"
        exit 1
    fi
    local val="$def"
    if [[ ${!var:-} ]]; then
        val="${!var}"
    elif [[ ${!fileVar:-} ]]; then
        val="$(< "${!fileVar}")"
    fi

    if [[ -n $val ]]; then
        export "$var"="$val"
    fi

    unset "$fileVar"
}

file_env 'SPRING_DATASOURCE_URL'
file_env 'SPRING_DATASOURCE_USERNAME'
file_env 'SPRING_DATASOURCE_PASSWORD'
file_env 'SPRING_LIQUIBASE_URL'
file_env 'SPRING_LIQUIBASE_USER'
file_env 'SPRING_LIQUIBASE_PASSWORD'
file_env 'JHIPSTER_REGISTRY_PASSWORD'
file_env 'DATABASE_DRIVER_STORAGE_PATH'

# Setup shared-drivers directory
SHARED_DRIVERS_DIR="${DATABASE_DRIVER_STORAGE_PATH:-/shared-drivers}"
echo "Setting up shared drivers directory: ${SHARED_DRIVERS_DIR}"

# Create directory structure if it doesn't exist
if [ ! -d "${SHARED_DRIVERS_DIR}" ]; then
    echo "Creating shared drivers directory: ${SHARED_DRIVERS_DIR}"
    mkdir -p "${SHARED_DRIVERS_DIR}"
fi

# Ensure proper permissions (readable and writable)
chmod -R 755 "${SHARED_DRIVERS_DIR}" 2>/dev/null || true

# Create subdirectories for each database vendor if they don't exist
mkdir -p "${SHARED_DRIVERS_DIR}/postgresql/16/jdbc" "${SHARED_DRIVERS_DIR}/postgresql/16/r2dbc" 2>/dev/null || true
mkdir -p "${SHARED_DRIVERS_DIR}/postgresql/15/jdbc" "${SHARED_DRIVERS_DIR}/postgresql/15/r2dbc" 2>/dev/null || true
mkdir -p "${SHARED_DRIVERS_DIR}/postgresql/14/jdbc" "${SHARED_DRIVERS_DIR}/postgresql/14/r2dbc" 2>/dev/null || true
mkdir -p "${SHARED_DRIVERS_DIR}/mysql/8.0/jdbc" "${SHARED_DRIVERS_DIR}/mysql/8.0/r2dbc" 2>/dev/null || true
mkdir -p "${SHARED_DRIVERS_DIR}/mysql/5.7/jdbc" "${SHARED_DRIVERS_DIR}/mysql/5.7/r2dbc" 2>/dev/null || true
mkdir -p "${SHARED_DRIVERS_DIR}/oracle/23c/jdbc" "${SHARED_DRIVERS_DIR}/oracle/23c/r2dbc" 2>/dev/null || true
mkdir -p "${SHARED_DRIVERS_DIR}/oracle/21c/jdbc" "${SHARED_DRIVERS_DIR}/oracle/21c/r2dbc" 2>/dev/null || true
mkdir -p "${SHARED_DRIVERS_DIR}/oracle/19c/jdbc" "${SHARED_DRIVERS_DIR}/oracle/19c/r2dbc" 2>/dev/null || true
mkdir -p "${SHARED_DRIVERS_DIR}/mssql/2022/jdbc" "${SHARED_DRIVERS_DIR}/mssql/2022/r2dbc" 2>/dev/null || true
mkdir -p "${SHARED_DRIVERS_DIR}/mssql/2019/jdbc" "${SHARED_DRIVERS_DIR}/mssql/2019/r2dbc" 2>/dev/null || true
mkdir -p "${SHARED_DRIVERS_DIR}/mssql/2017/jdbc" "${SHARED_DRIVERS_DIR}/mssql/2017/r2dbc" 2>/dev/null || true
mkdir -p "${SHARED_DRIVERS_DIR}/db2/11.5/jdbc" "${SHARED_DRIVERS_DIR}/db2/11.5/r2dbc" 2>/dev/null || true
mkdir -p "${SHARED_DRIVERS_DIR}/db2/11.1/jdbc" "${SHARED_DRIVERS_DIR}/db2/11.1/r2dbc" 2>/dev/null || true

echo "Shared drivers directory setup complete"

# Export the path for the application
export DATABASE_DRIVER_STORAGE_PATH="${SHARED_DRIVERS_DIR}"

# Wait for database to be ready (optional, but recommended)
# This helps ensure the database is available before Liquibase runs
if [ -n "${DB_HOST:-}" ] && [ -n "${DB_PORT:-}" ]; then
    echo "Checking database connectivity..."
    # Try to connect using available tools (nc, timeout, or just skip)
    if command -v nc >/dev/null 2>&1; then
        echo "Waiting for database at ${DB_HOST}:${DB_PORT} to be ready..."
        until nc -z "${DB_HOST}" "${DB_PORT}" 2>/dev/null; do
            echo "Database is unavailable - sleeping"
            sleep 2
        done
        echo "Database is ready!"
    elif command -v timeout >/dev/null 2>&1 && command -v bash >/dev/null 2>&1; then
        # Alternative: use bash with timeout (if available)
        echo "Waiting for database at ${DB_HOST}:${DB_PORT}..."
        for i in {1..30}; do
            if timeout 1 bash -c "echo > /dev/tcp/${DB_HOST}/${DB_PORT}" 2>/dev/null; then
                echo "Database is ready!"
                break
            fi
            echo "Database is unavailable - attempt $i/30"
            sleep 2
        done
    else
        echo "Database connectivity check skipped (nc/timeout not available)"
        echo "Spring Boot will handle connection retries automatically"
    fi
fi

# Ensure Liquibase is enabled (unless explicitly disabled)
if [ -z "${SPRING_LIQUIBASE_ENABLED:-}" ]; then
    export SPRING_LIQUIBASE_ENABLED=true
fi

echo "Starting application with Liquibase enabled: ${SPRING_LIQUIBASE_ENABLED}"

exec java ${JAVA_OPTS} -noverify -XX:+AlwaysPreTouch -Djava.security.egd=file:/dev/./urandom -cp /app/resources/:/app/classes/:/app/libs/* "com.atparui.rms.RmsApp"  "$@"
