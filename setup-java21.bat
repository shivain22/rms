@echo off
setlocal enabledelayedexpansion
REM Script to help configure Java 21 for this project
REM Run this after installing Java 21

echo Checking for Java 21 installation...
echo.

set JAVA21_HOME=

REM Check common installation paths for Temurin/Adoptium Java 21
for /d %%P in ("C:\Program Files\Eclipse Adoptium\jdk-21*") do (
    if exist "%%P\bin\java.exe" (
        set "JAVA21_HOME=%%P"
    )
)

if defined JAVA21_HOME goto :configure

for /d %%P in ("C:\Program Files\Java\jdk-21*") do (
    if exist "%%P\bin\java.exe" (
        set "JAVA21_HOME=%%P"
    )
)

if defined JAVA21_HOME goto :configure

for /d %%P in ("C:\Program Files (x86)\Eclipse Adoptium\jdk-21*") do (
    if exist "%%P\bin\java.exe" (
        set "JAVA21_HOME=%%P"
    )
)

if not defined JAVA21_HOME (
    echo ERROR: Java 21 not found in common installation locations.
    echo.
    echo Please install Java 21 from:
    echo   https://adoptium.net/temurin/releases/?version=21
    echo.
    echo Or use Chocolatey (as Administrator):
    echo   choco install temurin21 -y
    echo.
    echo After installation, manually set JAVA_HOME to your Java 21 installation path.
    pause
    exit /b 1
)

:configure
echo Found Java 21 at: !JAVA21_HOME!
echo.

REM Verify it's Java 21
"!JAVA21_HOME!\bin\java.exe" -version 2>&1 | findstr /C:"21" >nul
if errorlevel 1 (
    echo WARNING: The found Java installation may not be version 21.
    echo Please verify manually.
    echo.
)

echo Current JAVA_HOME: %JAVA_HOME%
echo.
echo To set JAVA_HOME for this session, run:
echo   set JAVA_HOME=!JAVA21_HOME!
echo   set PATH=%%JAVA_HOME%%\bin;%%PATH%%
echo.
echo To set JAVA_HOME permanently:
echo   1. Open System Properties (Win + Pause, then "Advanced system settings")
echo   2. Click "Environment Variables"
echo   3. Under "System variables", click "New" or edit existing JAVA_HOME
echo   4. Set Variable name: JAVA_HOME
echo   5. Set Variable value: !JAVA21_HOME!
echo   6. Click OK and restart your terminal/IDE
echo.
echo Setting JAVA_HOME for current session...
set "JAVA_HOME=!JAVA21_HOME!"
set "PATH=!JAVA_HOME!\bin;%PATH%"

echo.
echo Verifying Java version...
java -version
echo.
echo Verifying Maven uses Java 21...
mvn -version
echo.
echo Setup complete! You can now build the project with:
echo   mvn clean package
echo.
pause
endlocal
