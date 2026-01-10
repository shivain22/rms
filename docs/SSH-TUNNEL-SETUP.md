# SSH Tunnel Setup Guide for RMS Application

## Overview

This guide helps you set up SSH tunnels to connect your local RMS application to remote services.

## Required Services and Ports

The RMS application requires tunnels to these services:

| Service       | Local Port | Remote Port | Description           |
| ------------- | ---------- | ----------- | --------------------- |
| PostgreSQL    | 5433       | 5435        | Database server       |
| Kafka         | 9092       | 9295        | Message broker        |
| Consul        | 8500       | 8500        | Service discovery     |
| Elasticsearch | 9200       | 9200        | Search engine         |
| Keycloak      | 8080       | 9292        | Authentication server |

## Quick Setup

### 1. Configure SSH Connection

Copy the environment template and update with your server details:

```bash
copy .env.template .env
# Edit .env with your actual SSH server details
```

### 2. Test Connectivity (PowerShell)

```powershell
# Test current tunnel status
.\ssh-tunnel-manager.ps1 -Action test

# Setup tunnels (replace with your details)
.\ssh-tunnel-manager.ps1 -Action setup -SshHost your-server.com -SshUser your-username

# Stop tunnels
.\ssh-tunnel-manager.ps1 -Action stop
```

### 3. Setup Tunnels (Command Prompt)

```cmd
# Edit setup-ssh-tunnel.bat with your server details, then run:
setup-ssh-tunnel.bat
```

### 4. Test Tunnels

```powershell
# Run the test script
.\test-ssh-tunnel.ps1
```

## Manual SSH Command

If you prefer to run SSH manually:

```bash
ssh -N -L 5433:localhost:5435 -L 9092:localhost:9295 -L 8500:localhost:8500 -L 9200:localhost:9200 -L 8080:localhost:9292 your-username@your-server.com
```

## Start RMS Application

Once tunnels are established and tested:

```bash
# Start with local profile (uses tunneled connections)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Or set environment variables for Spring Boot
set SPRING_PROFILES_ACTIVE=local
./mvnw spring-boot:run
```

## Troubleshooting

### Common Issues:

1. **Connection refused**: Check if SSH server is accessible
2. **Port already in use**: Another process is using the local port
3. **Authentication failed**: Check username/password/key
4. **Remote service not accessible**: Service may not be running on remote server

### Check Port Usage:

```cmd
netstat -an | findstr :5433
netstat -an | findstr :9092
netstat -an | findstr :8500
netstat-an | findstr :9200
netstat -an | findstr :8080
```

### Kill Existing SSH Tunnels:

```powershell
Get-Process -Name "ssh" | Stop-Process -Force
```

## Environment Variables

The application supports these environment variables for SSH tunnel configuration:

- `SSH_TUNNEL_ENABLED=true`
- `SSH_TUNNEL_HOST=your-server.com`
- `SSH_TUNNEL_USERNAME=your-username`
- `SSH_TUNNEL_PASSWORD=your-password`
- `SSH_TUNNEL_PRIVATE_KEY=path/to/key`

## Security Notes

- Use SSH keys instead of passwords when possible
- Keep your SSH private keys secure
- Consider using SSH config file (~/.ssh/config) for connection settings
- Use strong passwords and enable 2FA on your SSH server
