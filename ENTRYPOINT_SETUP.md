# Entrypoint Script Setup

## Overview

The `entrypoint.sh` script is used as the Docker container entrypoint. It handles:

1. Environment variable loading from files (Docker secrets support)
2. Shared drivers directory setup
3. Database connectivity check (optional)
4. Liquibase configuration
5. Application startup

## Location

- **Source**: `src/main/docker/jib/entrypoint.sh`
- **Docker**: `/entrypoint.sh` (copied during JIB build)

## Features

### 1. Environment Variable Loading

Supports loading sensitive values from files (Docker secrets):

```bash
file_env 'SPRING_DATASOURCE_PASSWORD'
file_env 'SPRING_LIQUIBASE_PASSWORD'
```

This allows using `SPRING_DATASOURCE_PASSWORD_FILE` to point to a secret file.

### 2. Shared Drivers Directory Setup

Automatically creates and configures the shared drivers directory:

- Creates directory if it doesn't exist
- Sets up subdirectories for all database vendors
- Sets proper permissions (755)
- Exports `DATABASE_DRIVER_STORAGE_PATH` environment variable

### 3. Database Connectivity Check

Optionally waits for the database to be ready before starting the application:

- Checks if `DB_HOST` and `DB_PORT` are set
- Uses `nc` (netcat) if available
- Falls back to bash TCP check if `nc` is not available
- Skips check if neither tool is available (Spring Boot will retry)

### 4. Liquibase Configuration

Ensures Liquibase is enabled by default:

```bash
if [ -z "${SPRING_LIQUIBASE_ENABLED:-}" ]; then
    export SPRING_LIQUIBASE_ENABLED=true
fi
```

## Usage

The script is automatically used when building with JIB:

```bash
mvn clean package jib:build
```

Or when using Docker Compose, the entrypoint is set in the Dockerfile/JIB configuration.

## Environment Variables

| Variable                       | Description                          | Default           |
| ------------------------------ | ------------------------------------ | ----------------- |
| `DATABASE_DRIVER_STORAGE_PATH` | Path to shared drivers directory     | `/shared-drivers` |
| `DB_HOST`                      | Database host for connectivity check | -                 |
| `DB_PORT`                      | Database port for connectivity check | -                 |
| `SPRING_LIQUIBASE_ENABLED`     | Enable/disable Liquibase             | `true`            |
| `JHIPSTER_SLEEP`               | Sleep time before starting (seconds) | `0`               |

## Customization

To customize the entrypoint behavior:

1. **Modify the script**: Edit `src/main/docker/jib/entrypoint.sh`
2. **Rebuild**: Run `mvn clean package` to include changes
3. **Test**: Use `docker-compose up` to test locally

## Troubleshooting

### Drivers Directory Not Created

- Check Docker volume mount configuration
- Verify `DATABASE_DRIVER_STORAGE_PATH` is set correctly
- Check container logs for permission errors

### Database Connection Fails

- Verify `DB_HOST` and `DB_PORT` are correct
- Check network connectivity between containers
- Review Spring Boot connection retry logs

### Liquibase Not Running

- Check `SPRING_LIQUIBASE_ENABLED` is not set to `false`
- Verify database connection is working
- Review Liquibase logs in application startup
