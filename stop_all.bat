@echo off
setlocal enabledelayedexpansion

echo ==================================================
echo Stopping Automated Testing Platform
echo ==================================================

set "KILLED=0"

echo.
echo [Checking Backend port 8080]
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTEN') do (
    echo Killing process on port 8080 (PID: %%a)...
    taskkill /F /PID %%a >nul 2>nul
    set "KILLED=1"
)

echo.
echo [Checking Frontend port 5173]
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :5173 ^| findstr LISTEN') do (
    echo Killing process on port 5173 (PID: %%a)...
    taskkill /F /PID %%a >nul 2>nul
    set "KILLED=1"
)

echo.
if "!KILLED!"=="1" (
    echo Services stopped successfully.
) else (
    echo No running services found on ports 8080 or 5173.
)

pause
