@echo off
setlocal

echo ==================================================
echo Installing Dependencies
echo ==================================================

REM ---------------------------------------------------
REM Check for Maven
REM ---------------------------------------------------
set "MVN_CMD=mvn"
where mvn >nul 2>nul
if %errorlevel% equ 0 (
    echo System Maven found.
    goto :MavenFound
)

echo System Maven not found. Checking local portable Maven...
set "TOOLS_DIR=%~dp0.tools"
set "MAVEN_VERSION=3.9.6"
set "MAVEN_DIR=%TOOLS_DIR%\apache-maven-%MAVEN_VERSION%"
set "MVN_CMD=%MAVEN_DIR%\bin\mvn.cmd"

if exist "%MVN_CMD%" (
    echo Local portable Maven found.
    goto :MavenFound
)

echo.
echo Maven is missing! Attempting to download portable Maven %MAVEN_VERSION%...
if not exist "%TOOLS_DIR%" mkdir "%TOOLS_DIR%"

echo Downloading Maven... (This may take a while)
powershell -Command "$ProgressPreference = 'SilentlyContinue'; Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip' -OutFile '%TOOLS_DIR%\maven.zip'"

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Failed to download Maven.
    echo Please install Maven manually or check your internet connection.
    pause
    exit /b 1
)

echo Extracting Maven...
powershell -Command "Expand-Archive -Path '%TOOLS_DIR%\maven.zip' -DestinationPath '%TOOLS_DIR%' -Force"
del "%TOOLS_DIR%\maven.zip"

if not exist "%MVN_CMD%" (
    echo.
    echo [ERROR] Failed to configure local Maven.
    pause
    exit /b 1
)

echo Maven installed successfully to %MAVEN_DIR%

:MavenFound
echo Using Maven: %MVN_CMD%

REM ---------------------------------------------------
REM Install Backend
REM ---------------------------------------------------
echo.
echo [1/2] Installing Backend Dependencies...
cd platform-backed
call "%MVN_CMD%" clean install -DskipTests
if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Backend installation failed!
    pause
    exit /b %errorlevel%
)
cd ..

REM ---------------------------------------------------
REM Install Frontend
REM ---------------------------------------------------
echo.
echo [2/2] Installing Frontend Dependencies...
cd platform-frontend
call npm install
if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Frontend installation failed!
    pause
    exit /b %errorlevel%
)
cd ..

echo.
echo ==================================================
echo All dependencies installed successfully!
echo ==================================================
pause
