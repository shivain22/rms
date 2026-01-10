# SSH Tunnel Setup - Updated for Your Remote Server

## Remote Server Port Mapping (from your docker ps)

Based on your remote server's Docker containers, here are the correct port mappings:

| Service                   | Local Port | Remote Port | Container         | Description           |
| ------------------------- | ---------- | ----------- | ----------------- | --------------------- |
| **PostgreSQL (RMS)**      | 5433       | 5435        | rms-postgresql    | Main RMS database     |
| **PostgreSQL (Keycloak)** | 5434       | 5434        | keycloak-db       | Keycloak database     |
| **Kafka**                 | 9092       | 9295        | rms-kafka         | Message broker        |
| **Consul**                | 8500       | 8500        | rms-consul        | Service discovery     |
| **Elasticsearch**         | 9200       | 9200        | rms-elasticsearch | Search engine         |
| **Keycloak**              | 8080       | 9292        | keycloak          | Authentication server |
| **RMS Gateway**           | 9293       | 9293        | rms-gateway       | Gateway service       |
| **RMS Service**           | 9294       | 9294        | rms-service       | RMS microservice      |

## Quick Setup Steps

### 1. Edit SSH Connection Details

Edit `quick-tunnel.bat` and update:

```batch
set SSH_HOST=atparui-server  # or your server's IP/hostname
set SSH_USER=sivakumar       # or your SSH username
```

### 2. Run SSH Tunnel

```cmd
quick-tunnel.bat
```

### 3. Test Connectivity

In another terminal:

```powershell
.\ssh-tunnel-manager.ps1 -Action test
```

### 4. Start RMS Application

Once tunnels are working:

```cmd
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

## Manual SSH Command

If you prefer manual setup:

```bash
ssh -N \
  -L 5433:localhost:5435 \
  -L 5434:localhost:5434 \
  -L 9092:localhost:9295 \
  -L 8500:localhost:8500 \
  -L 9200:localhost:9200 \
  -L 8080:localhost:9292 \
  -L 9293:localhost:9293 \
  -L 9294:localhost:9294 \
  sivakumar@atparui-server
```

## Application Configuration

Your `application-local.yml` should use these local ports:

- Database: `localhost:5433` (tunnels to remote port 5435)
- Keycloak: `localhost:8080` (tunnels to remote port 9292)
- Kafka: `localhost:9092` (tunnels to remote port 9295)
- Consul: `localhost:8500` (tunnels to remote port 8500)
- Elasticsearch: `localhost:9200` (tunnels to remote port 9200)

## Verification Commands

```cmd
# Check if ports are listening
netstat -an | findstr ":5433 :5434 :9092 :8500 :9200 :8080 :9293 :9294"

# Test specific services
curl http://localhost:8500/v1/status/leader  # Consul
curl http://localhost:9200/_cluster/health   # Elasticsearch
curl http://localhost:8080/realms/gateway    # Keycloak
```

## Next Steps

1. Update `quick-tunnel.bat` with your SSH details
2. Run the tunnel
3. Test connectivity
4. Start your RMS application with `local` profile
