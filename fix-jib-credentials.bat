@echo off
REM Fix Jib authentication issues by refreshing Docker credentials

echo ========================================
echo Fixing Jib Docker Hub Authentication
echo ========================================
echo.

echo The issue: Jib is trying to use Docker Hub credentials that may have expired.
echo.
echo Solution options:
echo.
echo OPTION 1: Re-login to Docker Hub (Recommended)
echo ------------------------------------------------
echo 1. Make sure Docker Desktop is running
echo 2. Run: docker login
echo 3. Enter your Docker Hub username and password
echo 4. Then try building again: mvn clean package jib:build -Ddocker.password=YOUR_PASSWORD
echo.
echo OPTION 2: Use local build (No push needed)
echo ------------------------------------------
echo If you just want to build locally without pushing:
echo   mvn clean package jib:dockerBuild
echo This avoids registry authentication entirely.
echo.
echo OPTION 3: Clear and reset credentials
echo --------------------------------------
echo If credentials are corrupted, you can:
echo 1. Stop Docker Desktop
echo 2. Delete: %USERPROFILE%\.docker\config.json
echo 3. Start Docker Desktop
echo 4. Run: docker login
echo.
echo ========================================
echo.

REM Check if Docker Desktop is running
docker info >nul 2>&1
if errorlevel 1 (
    echo WARNING: Docker Desktop is not running!
    echo Please start Docker Desktop first, then try again.
    echo.
) else (
    echo Docker Desktop is running.
    echo.
    echo Current Docker login status:
    docker info 2>&1 | findstr /C:"Username" /C:"Registry"
    echo.
    echo To refresh credentials, run: docker login
)

echo.
echo To test if base image can be pulled:
echo   docker pull eclipse-temurin:21-jre-focal
echo.
pause

