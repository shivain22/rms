@echo off
REM Disable SSH Tunnels in RMS Application

echo ========================================
echo SSH Tunnel Control
echo ========================================
echo.
echo 1. Disable SSH tunnels (set environment variable)
echo 2. Enable SSH tunnels  
echo 3. Check tunnel status
echo 4. Kill all SSH processes
echo.
set /p choice="Select option (1-4): "

if "%choice%"=="1" goto DISABLE_TUNNELS
if "%choice%"=="2" goto ENABLE_TUNNELS  
if "%choice%"=="3" goto CHECK_STATUS
if "%choice%"=="4" goto KILL_SSH
goto END

:DISABLE_TUNNELS
echo Setting SSH_TUNNEL_ENABLED=false
set SSH_TUNNEL_ENABLED=false
setx SSH_TUNNEL_ENABLED false
echo SSH tunnels disabled. Restart your application to take effect.
goto END

:ENABLE_TUNNELS
echo Setting SSH_TUNNEL_ENABLED=true
set SSH_TUNNEL_ENABLED=true
setx SSH_TUNNEL_ENABLED true
echo SSH tunnels enabled. Restart your application to take effect.
goto END

:CHECK_STATUS
echo Current SSH tunnel setting:
echo SSH_TUNNEL_ENABLED=%SSH_TUNNEL_ENABLED%
echo.
echo SSH processes:
tasklist /FI "IMAGENAME eq ssh.exe" 2>nul || echo No SSH processes found
echo.
echo Java processes (may include RMS app with JSch tunnels):
tasklist /FI "IMAGENAME eq java.exe" /FO TABLE
goto END

:KILL_SSH
echo Killing all SSH processes...
taskkill /F /IM ssh.exe 2>nul || echo No SSH processes to kill
echo Done.
goto END

:END
pause