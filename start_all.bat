@echo off
setlocal enabledelayedexpansion

echo ==================================================
echo Starting Automated Testing Platform
echo ==================================================

REM ---------------------------------------------------
REM Check and Kill existing processes
REM ---------------------------------------------------
echo.
echo [Checking ports]

REM Check port 8080
netstat -ano | findstr ":8080" | findstr "LISTEN" > "%TEMP%\port_8080.txt"
for /f "tokens=5" %%a in ('type "%TEMP%\port_8080.txt"') do (
    if not "%%a"=="" (
        echo Killing process on port 8080 - PID: %%a
        taskkill /F /PID %%a >nul 2>nul
    )
)
if exist "%TEMP%\port_8080.txt" del "%TEMP%\port_8080.txt"

REM Check port 5173
netstat -ano | findstr ":5173" | findstr "LISTEN" > "%TEMP%\port_5173.txt"
for /f "tokens=5" %%a in ('type "%TEMP%\port_5173.txt"') do (
    if not "%%a"=="" (
        echo Killing process on port 5173 - PID: %%a
        taskkill /F /PID %%a >nul 2>nul
    )
)
if exist "%TEMP%\port_5173.txt" del "%TEMP%\port_5173.txt"

REM ---------------------------------------------------
REM Check for Maven
REM ---------------------------------------------------
set "MVN_CMD=mvn"
where mvn >nul 2>nul
if %errorlevel% equ 0 goto :MavenFound

set "TOOLS_DIR=%~dp0.tools"
set "MAVEN_VERSION=3.9.6"
set "MAVEN_DIR=%TOOLS_DIR%\apache-maven-%MAVEN_VERSION%"
set "LOCAL_MVN=%MAVEN_DIR%\bin\mvn.cmd"

if exist "%LOCAL_MVN%" (
    set "MVN_CMD=%LOCAL_MVN%"
    goto :MavenFound
)

echo.
echo [WARNING] Maven not found! 
echo Please run 'install_dependencies.bat' first to automatically install Maven.
echo.
pause
exit /b 1

:MavenFound
echo Using Maven: "%MVN_CMD%"

echo.
echo [1/2] Starting Backend (Spring Boot)...
cd /d "%~dp0platform-backed"
start "Platform Backend" cmd /k call "%MVN_CMD%" spring-boot:run

echo.
echo [2/2] Starting Frontend (Vue + Vite)...
cd /d "%~dp0platform-frontend"
start "Platform Frontend" cmd /k "npm run dev"

echo.
echo [Opening Microsoft Edge]
start "" msedge http://localhost:5173/

echo.
echo Both services are attempting to start in new windows.
echo.
pause
