# How to Run and Debug RMS Application in Cursor

This guide explains how to run and debug the RMS Spring Boot application in Cursor with the provided environment variables.

## Prerequisites

1. **Java 17** - Ensure Java 17 is installed and `JAVA_HOME` is set
2. **Maven** - Maven should be installed (or use the included `mvnw` wrapper)
3. **Node.js** - Node.js v22.15.0+ for frontend development
4. **PostgreSQL** - Database should be running on localhost:5432
5. **Consul** (optional) - If `CONSUL_ENABLED=true`, Consul should be running

## Method 1: Debug Using Cursor's Debugger (Recommended)

### Steps:

1. **Open the project** in Cursor
2. **Go to Run and Debug** (Ctrl+Shift+D or Cmd+Shift+D)
3. **Select "Debug Spring Boot Application"** from the dropdown
4. **Click the green play button** or press F5

The debugger will:

- Start the Spring Boot application with all environment variables configured
- Attach the debugger automatically
- Set breakpoints in your Java code

### Setting Breakpoints

- Click in the gutter (left of line numbers) to set breakpoints
- The debugger will pause execution at breakpoints
- Use the debug toolbar to step through code

## Method 2: Run from Terminal

### Windows (CMD):

```cmd
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

mvnw spring-boot:run
```

### Windows (PowerShell):

```powershell
$env:APP_PORT="8082"
$env:CONSUL_ENABLED="true"
$env:CONSUL_HOST="localhost"
$env:CONSUL_PORT="8500"
$env:CORS_ALLOWED_ORIGINS="http://localhost:3000,http://localhost:4200,http://localhost:8100,http://localhost:9000"
$env:DB_HOST="localhost"
$env:DB_NAME="rms"
$env:DB_PASSWORD="rms"
$env:DB_PORT="5432"
$env:DB_USERNAME="rms"
$env:ELASTICSEARCH_ENABLED="false"
$env:ELASTICSEARCH_HOST="localhost"
$env:ELASTICSEARCH_PORT="9200"
$env:KEYCLOAK_CLIENT_ID="gateway"
$env:KEYCLOAK_CLIENT_SECRET="M5nP8qR2sT6uV9wX1yZ3aC4dE7fG0h"
$env:KEYCLOAK_HOST="rmsauth.atparui.com"
$env:KEYCLOAK_REALM="gateway"
$env:LOG_LEVEL_APP="DEBUG"
$env:LOG_LEVEL_ROOT="INFO"
$env:MULTITENANCY_DEFAULT_TENANT="gateway"
$env:MULTITENANCY_ENABLED="true"
$env:MULTITENANCY_TENANT_HEADER="X-Tenant-ID"
$env:SPRING_DOCKER_COMPOSE_ENABLED="false"
$env:SPRING_PROFILES_ACTIVE="dev"

.\mvnw.cmd spring-boot:run
```

### Using Maven with Environment Variables:

```cmd
mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

## Method 3: Debug with Remote Debugging

If you want to run the application separately and attach the debugger:

1. **Start the application with debug enabled:**

   ```cmd
   mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8000"
   ```

2. **In Cursor, select "Debug Spring Boot (Attach)"** from the debug dropdown
3. **Click the play button** to attach the debugger

## Method 4: Using npm Scripts

The project includes npm scripts for convenience:

### Start Backend Only:

```cmd
npm run backend:start
```

### Start with Debug:

```cmd
npm run backend:debug
```

### Start Frontend (separate terminal):

```cmd
npm start
```

### Start Both (Backend + Frontend):

```cmd
npm run watch
```

## Frontend Development

The frontend runs separately on port 9000 (default):

```cmd
npm start
```

This starts the webpack dev server for the React frontend.

## Troubleshooting

### Java Version Issues

- Ensure Java 17 is installed: `java -version`
- Set `JAVA_HOME` environment variable

### Database Connection Issues

- Verify PostgreSQL is running: `pg_isready -h localhost -p 5432`
- Check database credentials match your setup
- Ensure database `rms` exists

### Port Already in Use

- Change `APP_PORT` to a different port (e.g., 8083)
- Or stop the process using port 8082

### Consul Connection Issues

- If `CONSUL_ENABLED=true`, ensure Consul is running on localhost:8500
- Or set `CONSUL_ENABLED=false` to disable Consul

### Keycloak Connection Issues

- Verify Keycloak is accessible at `rmsauth.atparui.com`
- Check network connectivity
- Verify client credentials are correct

## Environment Variables Reference

All environment variables are configured in `.vscode/launch.json` for debugging. Key variables:

- **APP_PORT**: Application server port (default: 8082)
- **SPRING_PROFILES_ACTIVE**: Spring profile (dev/prod)
- **DB\_\***: Database connection settings
- **KEYCLOAK\_\***: Keycloak authentication settings
- **MULTITENANCY\_\***: Multi-tenancy configuration
- **CONSUL\_\***: Consul service discovery settings

## Additional Resources

- Check `intellij-env-vars.txt` for IntelliJ IDEA configuration
- See `README.md` for project-specific documentation
- JHipster documentation: https://www.jhipster.tech/
