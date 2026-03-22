@echo off
setlocal enabledelayedexpansion

title Start Automated Testing Platform

echo ==================================================
echo Starting Automated Testing Platform
echo ==================================================

REM ---------------------------------------------------
REM Check and Kill existing processes
REM ---------------------------------------------------
echo.
echo [Checking ports]

REM Check port 8080
netstat -ano | findstr /i ":8080" > "%TEMP%\atp_start_port_8080.txt" 2>nul
for /f "tokens=5" %%a in ('type "%TEMP%\atp_start_port_8080.txt"') do (
    if not "%%a"=="" (
        echo Killing process on port 8080 - PID: %%a
        taskkill /F /PID %%a >nul 2>nul
    )
)
if exist "%TEMP%\atp_start_port_8080.txt" del "%TEMP%\atp_start_port_8080.txt"

REM Check port 5173
netstat -ano | findstr /i ":5173" > "%TEMP%\atp_start_port_5173.txt" 2>nul
for /f "tokens=5" %%a in ('type "%TEMP%\atp_start_port_5173.txt"') do (
    if not "%%a"=="" (
        echo Killing process on port 5173 - PID: %%a
        taskkill /F /PID %%a >nul 2>nul
    )
)
if exist "%TEMP%\atp_start_port_5173.txt" del "%TEMP%\atp_start_port_5173.txt"

REM Check port 18080
netstat -ano | findstr /i ":18080" > "%TEMP%\atp_start_port_18080.txt" 2>nul
for /f "tokens=5" %%a in ('type "%TEMP%\atp_start_port_18080.txt"') do (
    if not "%%a"=="" (
        echo Killing process on port 18080 - PID: %%a
        taskkill /F /PID %%a >nul 2>nul
    )
)
if exist "%TEMP%\atp_start_port_18080.txt" del "%TEMP%\atp_start_port_18080.txt"

REM Check port 18081
netstat -ano | findstr /i ":18081" > "%TEMP%\atp_start_port_18081.txt" 2>nul
for /f "tokens=5" %%a in ('type "%TEMP%\atp_start_port_18081.txt"') do (
    if not "%%a"=="" (
        echo Killing process on port 18081 - PID: %%a
        taskkill /F /PID %%a >nul 2>nul
    )
)
if exist "%TEMP%\atp_start_port_18081.txt" del "%TEMP%\atp_start_port_18081.txt"

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
start "Platform Backend" cmd /k "title Platform Backend & call "%MVN_CMD%" spring-boot:run"

echo.
echo [2/2] Starting Frontend (Vue + Vite)...
cd /d "%~dp0platform-frontend"
set "BACKEND_PORT="
for /L %%T in (1,1,60) do (
  for /L %%P in (18080,1,18100) do (
    netstat -ano | findstr /i ":%%P" >nul 2>nul
    if not errorlevel 1 (
      set "BACKEND_PORT=%%P"
      goto :FOUND_BACKEND_PORT
    )
  )
  ping 127.0.0.1 -n 2 >nul
)
:FOUND_BACKEND_PORT
if "%BACKEND_PORT%"=="" set "BACKEND_PORT=18080"
echo Detected backend port: %BACKEND_PORT%
echo.
echo [Frontend Check] Starting dev server directly...
set "VITE_API_TARGET=http://127.0.0.1:%BACKEND_PORT%"
start "Platform Frontend" cmd /k "title Platform Frontend & set VITE_API_TARGET=%VITE_API_TARGET% && npm run dev"

echo.
echo [Opening Microsoft Edge]
start msedge http://localhost:5173/

echo.
echo Both services are attempting to start in new windows.
echo.
pause
