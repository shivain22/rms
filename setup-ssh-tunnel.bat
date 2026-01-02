@echo off
REM SSH Tunnel Setup Script for RMS Application
REM This script establishes SSH tunnels for all required services

echo ========================================
echo RMS SSH Tunnel Setup
echo ========================================

REM Check if SSH is available
where ssh >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: SSH client not found. Please install OpenSSH or Git Bash.
    pause
    exit /b 1
)

REM Configuration - Update these values for your environment
set SSH_HOST=your-server.com
set SSH_USER=your-username
set SSH_PORT=22

REM Check if configuration is set
if "%SSH_HOST%"=="your-server.com" (
    echo ERROR: Please update SSH_HOST in this script with your actual server address
    echo Edit setup-ssh-tunnel.bat and set SSH_HOST=your-actual-server.com
    pause
    exit /b 1
)

if "%SSH_USER%"=="your-username" (
    echo ERROR: Please update SSH_USER in this script with your actual username
    echo Edit setup-ssh-tunnel.bat and set SSH_USER=your-actual-username
    pause
    exit /b 1
)

echo SSH Configuration:
echo   Host: %SSH_HOST%
echo   User: %SSH_USER%
echo   Port: %SSH_PORT%
echo.

echo Setting up SSH tunnels...
echo.

REM Create SSH tunnel with multiple port forwards
echo Establishing SSH tunnels for all services...
ssh -N -L 5433:localhost:5435 -L 9092:localhost:9295 -L 8500:localhost:8500 -L 9200:localhost:9200 -L 8080:localhost:9292 %SSH_USER%@%SSH_HOST% -p %SSH_PORT%

echo SSH tunnels closed.
pause