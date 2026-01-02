@echo off
setlocal enabledelayedexpansion

REM ========================================
REM RMS SSH Tunnel Manager (Windows)
REM ========================================

:MAIN_MENU
cls
echo ========================================
echo RMS SSH Tunnel Manager
echo ========================================
echo.
echo 1. Setup SSH Tunnels
echo 2. Test Tunnel Connectivity
echo 3. Stop SSH Tunnels
echo 4. Show Port Status
echo 5. Exit
echo.
set /p choice="Select an option (1-5): "

if "%choice%"=="1" goto SETUP_TUNNELS
if "%choice%"=="2" goto TEST_TUNNELS
if "%choice%"=="3" goto STOP_TUNNELS
if "%choice%"=="4" goto SHOW_PORTS
if "%choice%"=="5" goto EXIT
goto MAIN_MENU

:SETUP_TUNNELS
cls
echo ========================================
echo SSH Tunnel Setup
echo ========================================
echo.

REM Check if SSH is available
where ssh >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: SSH client not found!
    echo Please install OpenSSH or Git for Windows.
    pause
    goto MAIN_MENU
)

echo SSH client found: 
where ssh
echo.

REM Get SSH connection details
set /p SSH_HOST="Enter SSH hostname/IP: "
set /p SSH_USER="Enter SSH username: "
set /p SSH_PORT="Enter SSH port (default 22): "
if "%SSH_PORT%"=="" set SSH_PORT=22

echo.
echo Configuration:
echo   Host: %SSH_HOST%
echo   User: %SSH_USER%
echo   Port: %SSH_PORT%
echo.
echo Port Forwarding:
echo   PostgreSQL (RMS):     localhost:5433 ^<-^> %SSH_HOST%:5435
echo   PostgreSQL (Keycloak): localhost:5434 ^<-^> %SSH_HOST%:5434
echo   Kafka:                 localhost:9092 ^<-^> %SSH_HOST%:9295
echo   Consul:                localhost:8500 ^<-^> %SSH_HOST%:8500
echo   Elasticsearch:         localhost:9200 ^<-^> %SSH_HOST%:9200
echo   Keycloak:              localhost:8080 ^<-^> %SSH_HOST%:9292
echo   RMS Gateway:           localhost:9293 ^<-^> %SSH_HOST%:9293
echo   RMS Service:           localhost:9294 ^<-^> %SSH_HOST%:9294
echo.

set /p confirm="Continue with tunnel setup? (y/n): "
if /i not "%confirm%"=="y" goto MAIN_MENU

echo.
echo Starting SSH tunnels...
echo Press Ctrl+C to stop the tunnels
echo.

ssh -N -L 5433:localhost:5435 -L 5434:localhost:5434 -L 9092:localhost:9295 -L 8500:localhost:8500 -L 9200:localhost:9200 -L 8080:localhost:9292 -L 9293:localhost:9293 -L 9294:localhost:9294 %SSH_USER%@%SSH_HOST% -p %SSH_PORT%

echo.
echo SSH tunnels disconnected.
pause
goto MAIN_MENU

:TEST_TUNNELS
cls
echo ========================================
echo Testing SSH Tunnel Connectivity
echo ========================================
echo.

REM Test each port
call :TEST_PORT "PostgreSQL (RMS)" 5433
call :TEST_PORT "PostgreSQL (Keycloak)" 5434
call :TEST_PORT "Kafka" 9092
call :TEST_PORT "Consul" 8500
call :TEST_PORT "Elasticsearch" 9200
call :TEST_PORT "Keycloak" 8080
call :TEST_PORT "RMS Gateway" 9293
call :TEST_PORT "RMS Service" 9294

echo.
echo Test completed.
pause
goto MAIN_MENU

:TEST_PORT
set service=%~1
set port=%~2
echo Testing %service% (port %port%)...

REM Use PowerShell to test the port
powershell -Command "try { $tcpClient = New-Object System.Net.Sockets.TcpClient; $result = $tcpClient.BeginConnect('localhost', %port%, $null, $null); $success = $result.AsyncWaitHandle.WaitOne(3000, $false); if ($success -and $tcpClient.Connected) { Write-Host '  [OK] %service% is accessible' -ForegroundColor Green; $tcpClient.Close() } else { Write-Host '  [FAILED] %service% is not accessible' -ForegroundColor Red } } catch { Write-Host '  [FAILED] %service% connection error' -ForegroundColor Red } finally { if ($tcpClient) { $tcpClient.Close() } }"

goto :eof

:STOP_TUNNELS
cls
echo ========================================
echo Stopping SSH Tunnels
echo ========================================
echo.

REM Find and kill SSH processes
for /f "tokens=2" %%i in ('tasklist /FI "IMAGENAME eq ssh.exe" /FO CSV ^| find "ssh.exe"') do (
    echo Stopping SSH process %%i
    taskkill /PID %%i /F >nul 2>nul
)

echo SSH tunnels stopped.
pause
goto MAIN_MENU

:SHOW_PORTS
cls
echo ========================================
echo Port Status
echo ========================================
echo.

echo Checking required ports...
echo.

netstat -an | findstr ":5433" && echo PostgreSQL (RMS) port 5433: IN USE || echo PostgreSQL (RMS) port 5433: Available
netstat -an | findstr ":5434" && echo PostgreSQL (Keycloak) port 5434: IN USE || echo PostgreSQL (Keycloak) port 5434: Available
netstat -an | findstr ":9092" && echo Kafka port 9092: IN USE || echo Kafka port 9092: Available  
netstat -an | findstr ":8500" && echo Consul port 8500: IN USE || echo Consul port 8500: Available
netstat -an | findstr ":9200" && echo Elasticsearch port 9200: IN USE || echo Elasticsearch port 9200: Available
netstat -an | findstr ":8080" && echo Keycloak port 8080: IN USE || echo Keycloak port 8080: Available
netstat -an | findstr ":9293" && echo RMS Gateway port 9293: IN USE || echo RMS Gateway port 9293: Available
netstat -an | findstr ":9294" && echo RMS Service port 9294: IN USE || echo RMS Service port 9294: Available

echo.
echo SSH processes:
tasklist /FI "IMAGENAME eq ssh.exe" 2>nul || echo No SSH processes running

echo.
pause
goto MAIN_MENU

:EXIT
echo.
echo Goodbye!
exit /b 0