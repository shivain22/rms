# Enabling Keycloak Debug Logging

## Method 1: Using Keycloak Admin Console (Recommended)

1. Log into Keycloak Admin Console
2. Go to **Realm Settings** → **Events** → **Config**
3. Enable **Save Events** and set **Event Listeners** to include `jboss-logging`
4. Set **Saved Types** to include all event types
5. Go to **Realm Settings** → **Logging**
6. Add a new logger:
   - **Logger Name**: `org.keycloak`
   - **Level**: `DEBUG`
   - **Appender**: `console` or `file`

## Method 2: Using Keycloak Configuration File

Edit `standalone.xml` or `standalone-ha.xml` in your Keycloak installation:

```xml
<subsystem xmlns="urn:jboss:domain:logging:8.0">
    <logger category="org.keycloak">
        <level name="DEBUG"/>
    </logger>
    <logger category="org.keycloak.services.resources.admin">
        <level name="DEBUG"/>
    </logger>
    <logger category="org.keycloak.authentication">
        <level name="DEBUG"/>
    </logger>
    <logger category="org.keycloak.models">
        <level name="DEBUG"/>
    </logger>
</subsystem>
```

## Method 3: Using Environment Variables (Docker/Container)

If running Keycloak in Docker:

```bash
docker run -e KEYCLOAK_LOGLEVEL=DEBUG ...
```

Or add to your docker-compose.yml:

```yaml
environment:
  - KEYCLOAK_LOGLEVEL=DEBUG
```

## Method 4: Using Java System Properties

Add to your Keycloak startup script:

```bash
-Dorg.jboss.logging.provider=slf4j
-Dorg.keycloak.logging.level=DEBUG
```

## Specific Loggers for Flow Issues

For authentication flow debugging, enable these specific loggers:

```xml
<logger category="org.keycloak.authentication">
    <level name="DEBUG"/>
</logger>
<logger category="org.keycloak.models">
    <level name="DEBUG"/>
</logger>
<logger category="org.keycloak.services.resources.admin.AuthenticationManagementResource">
    <level name="TRACE"/>
</logger>
```

## Viewing Logs

- **Standalone**: Check `standalone/log/server.log`
- **Docker**: `docker logs <container-name>`
- **Kubernetes**: `kubectl logs <pod-name>`

## Filtering Logs

To filter for authentication flow related errors:

```bash
# Linux/Mac
grep -i "authentication\|flow\|execution" server.log

# Windows PowerShell
Select-String -Path server.log -Pattern "authentication|flow|execution" -CaseSensitive:$false
```
