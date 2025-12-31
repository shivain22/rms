@echo off
REM Script to fix Docker Hub credentials for Jib

echo Fixing Docker Hub credentials...
echo.

echo Step 1: Checking Docker login status...
docker info 2>&1 | findstr /C:"Username" >nul
if errorlevel 1 (
    echo Docker is not logged in or credentials are missing.
    echo.
    echo Step 2: Please log in to Docker Hub...
    echo You will be prompted for your Docker Hub username and password.
    echo.
    docker login
    if errorlevel 1 (
        echo.
        echo ERROR: Docker login failed. Please check your credentials.
        pause
        exit /b 1
    )
) else (
    echo Docker appears to be logged in.
    echo.
    echo Step 2: Re-authenticating with Docker Hub to refresh credentials...
    docker logout
    docker login
    if errorlevel 1 (
        echo.
        echo ERROR: Docker login failed. Please check your credentials.
        pause
        exit /b 1
    )
)

echo.
echo Step 3: Verifying credentials...
docker pull eclipse-temurin:21-jre-focal
if errorlevel 1 (
    echo.
    echo WARNING: Could not pull base image. This might be a network issue.
    echo However, Jib might still work. Try building now.
) else (
    echo.
    echo SUCCESS: Base image pulled successfully! Credentials are working.
)

echo.
echo Step 4: You can now try building with Jib:
echo   mvn clean package jib:build -Ddocker.password=YOUR_PASSWORD
echo.
echo Or for local build (no push needed):
echo   mvn clean package jib:dockerBuild
echo.
pause

