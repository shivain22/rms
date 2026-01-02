# SSH Tunnel Port Test Script for RMS Application
# This script tests connectivity to all tunneled ports

Write-Host "========================================" -ForegroundColor Green
Write-Host "RMS SSH Tunnel Port Test" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""

# Define the ports to test
$ports = @{
    "PostgreSQL" = 5433
    "Kafka" = 9092
    "Consul" = 8500
    "Elasticsearch" = 9200
    "Keycloak" = 8080
}

$allPortsOpen = $true

foreach ($service in $ports.Keys) {
    $port = $ports[$service]
    Write-Host "Testing $service (port $port)..." -NoNewline
    
    try {
        $tcpClient = New-Object System.Net.Sockets.TcpClient
        $tcpClient.ReceiveTimeout = 3000
        $tcpClient.SendTimeout = 3000
        
        $result = $tcpClient.BeginConnect("localhost", $port, $null, $null)
        $success = $result.AsyncWaitHandle.WaitOne(3000, $false)
        
        if ($success -and $tcpClient.Connected) {
            Write-Host " [OK]" -ForegroundColor Green
            $tcpClient.Close()
        } else {
            Write-Host " [FAILED]" -ForegroundColor Red
            $allPortsOpen = $false
        }
    } catch {
        Write-Host " [FAILED]" -ForegroundColor Red
        $allPortsOpen = $false
    } finally {
        if ($tcpClient) {
            $tcpClient.Close()
        }
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green

if ($allPortsOpen) {
    Write-Host "All SSH tunnels are working correctly!" -ForegroundColor Green
    Write-Host ""
    Write-Host "You can now start your RMS application with:" -ForegroundColor Yellow
    Write-Host "  ./mvnw spring-boot:run -Dspring-boot.run.profiles=local" -ForegroundColor Cyan
} else {
    Write-Host "Some SSH tunnels are not working!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please check:" -ForegroundColor Yellow
    Write-Host "1. SSH tunnel is running (setup-ssh-tunnel.bat)" -ForegroundColor White
    Write-Host "2. Remote services are running on the server" -ForegroundColor White
    Write-Host "3. Firewall settings allow the connections" -ForegroundColor White
}

Write-Host ""
Write-Host "Press any key to continue..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")