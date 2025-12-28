#!/bin/bash

# Setup script for local development

echo "Setting up RMS Multi-tenant Application..."

# Load environment variables
if [ -f .env.local ]; then
    export $(cat .env.local | grep -v '^#' | xargs)
    echo "Loaded .env.local"
else
    echo "Warning: .env.local not found, using default values"
fi

# Start local services
echo "Starting local PostgreSQL and other services..."
docker-compose -f docker-compose-local.yml --env-file .env.local up -d postgresql consul elasticsearch

# Wait for PostgreSQL to be ready
echo "Waiting for PostgreSQL to be ready..."
sleep 10

# Run database migrations
echo "Running database migrations..."
./mvnw liquibase:update -Dliquibase.url=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME} -Dliquibase.username=${DB_USERNAME} -Dliquibase.password=${DB_PASSWORD}
./mvnw liquibase:update -Dliquibase.url=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_TENANT1_NAME} -Dliquibase.username=${DB_USERNAME} -Dliquibase.password=${DB_PASSWORD}
./mvnw liquibase:update -Dliquibase.url=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_TENANT2_NAME} -Dliquibase.username=${DB_USERNAME} -Dliquibase.password=${DB_PASSWORD}

echo "Setup complete! You can now run the application in IntelliJ with the environment variables from .env.local"