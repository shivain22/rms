# RMS SSH Tunnel Manager
# This script sets up and manages SSH tunnels for RMS application

param(
    [string]$Action = "setup",
    [string]$SshHost = "",
    [string]$SshUser = "",
    [int]$SshPort = 22,
    [string]$SshKey = ""
)

function Show-Usage {
    Write-Host "RMS SSH Tunnel Manager" -ForegroundColor Green
    Write-Host "Usage: .\ssh-tunnel-manager.ps1 -Action <setup|test|stop> [options]" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Actions:" -ForegroundColor Cyan
    Write-Host "  setup   - Establish SSH tunnels"
    Write-Host "  test    - Test tunnel connectivity"
    Write-Host "  stop    - Stop SSH tunnels"
    Write-Host ""
    Write-Host "Options:" -ForegroundColor Cyan
    Write-Host "  -SshHost <hostname>   - SSH server hostname"
    Write-Host "  -SshUser <username>   - SSH username"
    Write-Host "  -SshPort <port>       - SSH port (default: 22)"
    Write-Host "  -SshKey <path>        - Path to SSH private key"
    Write-Host ""
    Write-Host "Examples:" -ForegroundColor Yellow
    Write-Host "  .\ssh-tunnel-manager.ps1 -Action setup -SshHost server.com -SshUser myuser"
    Write-Host "  .\ssh-tunnel-manager.ps1 -Action test"
    Write-Host "  .\ssh-tunnel-manager.ps1 -Action stop"
}

function Test-Ports {
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "Testing SSH Tunnel Connectivity" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""

    $ports = @{
        "PostgreSQL (RMS)" = 5433
        "PostgreSQL (Keycloak)" = 5434
        "Kafka" = 9092
        "Consul" = 8500
        "Elasticsearch" = 9200
        "Keycloak" = 8080
        "RMS Gateway" = 9293
        "RMS Service" = 9294
    }

    $results = @{}
    
    foreach ($service in $ports.Keys) {
        $port = $ports[$service]
        Write-Host "Testing $service (localhost:$port)..." -NoNewline
        
        try {
            $tcpClient = New-Object System.Net.Sockets.TcpClient
            $tcpClient.ReceiveTimeout = 3000
            $tcpClient.SendTimeout = 3000
            
            $result = $tcpClient.BeginConnect("localhost", $port, $null, $null)
            $success = $result.AsyncWaitHandle.WaitOne(3000, $false)
            
            if ($success -and $tcpClient.Connected) {
                Write-Host " [OK]" -ForegroundColor Green
                $results[$service] = $true
                $tcpClient.Close()
            } else {
                Write-Host " [FAILED]" -ForegroundColor Red
                $results[$service] = $false
            }
        } catch {
            Write-Host " [FAILED - $($_.Exception.Message)]" -ForegroundColor Red
            $results[$service] = $false
        } finally {
            if ($tcpClient) {
                $tcpClient.Close()
            }
        }
    }

    Write-Host ""
    Write-Host "Summary:" -ForegroundColor Cyan
    $successCount = ($results.Values | Where-Object { $_ -eq $true }).Count
    $totalCount = $results.Count
    
    Write-Host "  $successCount/$totalCount services accessible" -ForegroundColor $(if ($successCount -eq $totalCount) { "Green" } else { "Yellow" })
    
    if ($successCount -eq $totalCount) {
        Write-Host ""
        Write-Host "All tunnels working! You can start RMS with:" -ForegroundColor Green
        Write-Host "  ./mvnw spring-boot:run -Dspring-boot.run.profiles=local" -ForegroundColor Cyan
    } else {
        Write-Host ""
        Write-Host "Some services are not accessible. Check:" -ForegroundColor Yellow
        Write-Host "1. SSH tunnel is running" -ForegroundColor White
        Write-Host "2. Remote services are running" -ForegroundColor White
        Write-Host "3. Port forwarding is correct" -ForegroundColor White
    }
    
    return $successCount -eq $totalCount
}

function Setup-Tunnel {
    param($SshHost, $SshUser, $SshPort, $SshKey)
    
    if (-not $SshHost -or -not $SshUser) {
        Write-Host "Error: SSH host and user are required for setup" -ForegroundColor Red
        Show-Usage
        return $false
    }
    
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "Setting up SSH Tunnels" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Configuration:" -ForegroundColor Cyan
    Write-Host "  Host: $SshHost"
    Write-Host "  User: $SshUser"
    Write-Host "  Port: $SshPort"
    if ($SshKey) {
        Write-Host "  Key:  $SshKey"
    }
    Write-Host ""
    
    # Build SSH command
    $sshArgs = @(
        "-N"  # Don't execute remote command
        "-L", "5433:localhost:5435"    # PostgreSQL (RMS)
        "-L", "5434:localhost:5434"    # PostgreSQL (Keycloak)
        "-L", "9092:localhost:9295"    # Kafka
        "-L", "8500:localhost:8500"    # Consul
        "-L", "9200:localhost:9200"    # Elasticsearch
        "-L", "8080:localhost:9292"    # Keycloak
        "-L", "9293:localhost:9293"    # RMS Gateway
        "-L", "9294:localhost:9294"    # RMS Service
        "-p", $SshPort
    )
    
    if ($SshKey) {
        $sshArgs += @("-i", $SshKey)
    }
    
    $sshArgs += "$SshUser@$SshHost"
    
    Write-Host "Port Forwarding:" -ForegroundColor Cyan
    Write-Host "  PostgreSQL (RMS):     localhost:5433 -> $SshHost:5435"
    Write-Host "  PostgreSQL (Keycloak): localhost:5434 -> $SshHost:5434"
    Write-Host "  Kafka:                 localhost:9092 -> $SshHost:9295"
    Write-Host "  Consul:                localhost:8500 -> $SshHost:8500"
    Write-Host "  Elasticsearch:         localhost:9200 -> $SshHost:9200"
    Write-Host "  Keycloak:              localhost:8080 -> $SshHost:9292"
    Write-Host "  RMS Gateway:           localhost:9293 -> $SshHost:9293"
    Write-Host "  RMS Service:           localhost:9294 -> $SshHost:9294"
    Write-Host ""
    
    Write-Host "Starting SSH tunnel..." -ForegroundColor Yellow
    Write-Host "Press Ctrl+C to stop the tunnel" -ForegroundColor Yellow
    Write-Host ""
    
    try {
        & ssh $sshArgs
    } catch {
        Write-Host "Error starting SSH tunnel: $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
    
    return $true
}

function Stop-Tunnel {
    Write-Host "Stopping SSH tunnels..." -ForegroundColor Yellow
    
    # Find and kill SSH processes with port forwarding
    $sshProcesses = Get-Process -Name "ssh" -ErrorAction SilentlyContinue | Where-Object {
        $_.CommandLine -like "*-L*5433*" -or 
        $_.CommandLine -like "*-L*9092*" -or 
        $_.CommandLine -like "*-L*8500*"
    }
    
    if ($sshProcesses) {
        foreach ($process in $sshProcesses) {
            Write-Host "Stopping SSH process (PID: $($process.Id))"
            Stop-Process -Id $process.Id -Force
        }
        Write-Host "SSH tunnels stopped" -ForegroundColor Green
    } else {
        Write-Host "No SSH tunnel processes found" -ForegroundColor Yellow
    }
}

# Main execution
switch ($Action.ToLower()) {
    "setup" {
        Setup-Tunnel -SshHost $SshHost -SshUser $SshUser -SshPort $SshPort -SshKey $SshKey
    }
    "test" {
        Test-Ports
    }
    "stop" {
        Stop-Tunnel
    }
    default {
        Show-Usage
    }
}