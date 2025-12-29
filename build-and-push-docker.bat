@echo off
REM Build and push rms-gateway Docker image to Docker Hub
REM Usage: build-and-push-docker.bat [version]
REM If version is not provided, uses 'latest'

set VERSION=%1
if "%VERSION%"=="" set VERSION=latest

echo Building and pushing rms-gateway:%VERSION% to Docker Hub...
echo.

REM Set Docker password (you can also set this as an environment variable)
set DOCKER_PASSWORD=Asd!@#123

REM Build and push using Jib
call mvnw.cmd clean package jib:build -Ddocker.password=%DOCKER_PASSWORD% -Ddocker.image.tag=%VERSION%

if %ERRORLEVEL% EQU 0 (
    echo.
    echo Successfully built and pushed rms-gateway:%VERSION% to Docker Hub!
    echo Image: docker.io/shivain22/rms-gateway:%VERSION%
) else (
    echo.
    echo Failed to build and push Docker image.
    exit /b %ERRORLEVEL%
)

