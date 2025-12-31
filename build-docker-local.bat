@echo off
REM Build Docker image locally without pushing to registry
REM This avoids authentication issues with base image

echo Building Docker image locally...
echo.

call mvnw.cmd clean package jib:dockerBuild

if %ERRORLEVEL% EQU 0 (
    echo.
    echo Successfully built Docker image locally!
    echo Image: docker.io/shivain22/rms-gateway:latest
    echo.
    echo To run the image:
    echo   docker run -p 8080:8080 docker.io/shivain22/rms-gateway:latest
) else (
    echo.
    echo Failed to build Docker image.
    exit /b %ERRORLEVEL%
)

