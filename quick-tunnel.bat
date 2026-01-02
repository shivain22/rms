@echo off
REM Quick SSH Tunnel Setup for RMS - Based on your remote server ports
REM Update SSH_HOST and SSH_USER below with your actual values

set SSH_HOST=203.192.210.121
set SSH_USER=sivakumar

REM Check if configuration is set
if "%SSH_HOST%"=="your-server-hostname" (
    echo ERROR: Please update SSH_HOST in this script
    echo Edit this file and set SSH_HOST to your actual server hostname
    pause
    exit /b 1
)

if "%SSH_USER%"=="your-username" (
    echo ERROR: Please update SSH_USER in this script
    echo Edit this file and set SSH_USER to your actual username
    pause
    exit /b 1
)

echo ========================================
echo RMS SSH Tunnel - Quick Setup
echo ========================================
echo.
echo Connecting to: %SSH_USER%@%SSH_HOST%
echo.
echo Port Forwarding (based on your docker ps):
echo   PostgreSQL (RMS):      localhost:5433 -> %SSH_HOST%:5435
echo   PostgreSQL (Keycloak): localhost:5434 -> %SSH_HOST%:5434
echo   Kafka:                 localhost:9092 -> %SSH_HOST%:9295
echo   Consul:                localhost:8500 -> %SSH_HOST%:8500
echo   Elasticsearch:         localhost:9200 -> %SSH_HOST%:9200
echo   Keycloak:              localhost:8080 -> %SSH_HOST%:9292
echo   RMS Gateway:           localhost:9293 -> %SSH_HOST%:9293
echo   RMS Service:           localhost:9294 -> %SSH_HOST%:9294
echo.
echo Starting SSH tunnels... (Press Ctrl+C to stop)
echo.

ssh -N ^
  -L 5433:localhost:5435 ^
  -L 5434:localhost:5434 ^
  -L 9092:localhost:9295 ^
  -L 8500:localhost:8500 ^
  -L 9200:localhost:9200 ^
  -L 8080:localhost:9292 ^
  -L 9293:localhost:9293 ^
  -L 9294:localhost:9294 ^
  %SSH_USER%@%SSH_HOST%

echo.
echo SSH tunnels disconnected.
pause
