@echo off
setlocal enabledelayedexpansion

title Stop Automated Testing Platform

echo ==================================================
echo Stopping Automated Testing Platform
echo ==================================================

set "KILLED=0"
set "NO_PAUSE=0"
if /I "%~1"=="--pause" set "NO_PAUSE=0"
if /I "%~1"=="/pause" set "NO_PAUSE=0"
if /I "%~1"=="--nopause" set "NO_PAUSE=1"
if /I "%~1"=="/nopause" set "NO_PAUSE=1"

call :KillPort 18080 Backend
call :KillPort 18081 Backend
call :KillPort 8080 Backend
call :KillPort 5173 Frontend
call :KillPort 4723 Appium

call :KillWindow "Start Automated Testing Platform"
call :KillWindow "Platform Backend"
call :KillWindow "Platform Frontend"
call :KillWindow "npm run dev"
call :KillWindow "npm run check"

echo.
if "!KILLED!"=="1" (
    echo Services stopped successfully.
) else (
    echo No running services found.
)

if "!NO_PAUSE!"=="0" pause
exit /b 0

:KillPort
set "PORT=%~1"
set "LABEL=%~2"
echo.
echo [Checking %LABEL% port %PORT%]
set "PORT_FILE=%TEMP%\atp_stop_port_%PORT%.txt"
netstat -ano | findstr /i ":%PORT%" > "%PORT_FILE%" 2>nul
for /f "tokens=5" %%a in ('type "%PORT_FILE%"') do (
    if not "%%a"=="" (
        echo Killing process on port %PORT% [PID: %%a]...
        taskkill /F /T /PID %%a >nul 2>nul
        if not errorlevel 1 set "KILLED=1"
    )
)
del "%PORT_FILE%" >nul 2>nul
exit /b 0

:KillWindow
set "TITLE=%~1"
echo.
echo [Closing windows: %TITLE%]
taskkill /F /T /FI "WINDOWTITLE eq %TITLE%*" >nul 2>nul
if not errorlevel 1 set "KILLED=1"
exit /b 0
