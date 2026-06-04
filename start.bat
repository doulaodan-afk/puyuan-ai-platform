@echo off
chcp 65001 > nul

echo ========================================
echo Puyuan AI Platform - Start All Services
echo ========================================

REM Configuration
set MAX_RETRIES=3
set RETRY_DELAY=5
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot
set MAVEN_HOME=D:\maven\apache-maven-3.9.16
set PROJECT_DIR=D:\puyuanmaoshan
set BACKEND_DIR=%PROJECT_DIR%\backend\java-spring
set MERCHANT_DIR=%PROJECT_DIR%\frontend\merchant-web
set ADMIN_DIR=%PROJECT_DIR%\frontend\admin-web

REM Set PATH (use quotes to handle spaces in JAVA_HOME)
set "PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%"

REM 0. Verify build environment
echo.
echo [0/7] Verifying build environment...

where mvn >nul 2>&1
if %errorlevel% neq 0 (
    if exist "%MAVEN_HOME%\bin\mvn.cmd" (
        set "MVN=%MAVEN_HOME%\bin\mvn.cmd"
    ) else (
        echo [ERROR] Maven not found. Please install Maven or add to PATH.
        pause
        exit /b 1
    )
) else (
    set "MVN=mvn"
)

where java >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java not found. Please install JDK 17 or add to PATH.
    pause
    exit /b 1
)
echo [OK] Java and Maven found

REM 1. Check MySQL is running
echo.
echo [1/7] Checking MySQL...

REM Try common MySQL service names
set MYSQL_FOUND=0
for %%S in (MySQL84 MySQL80 MySQL MySQL57) do (
    sc query %%S 2>nul | findstr "RUNNING" > nul 2>&1
    if %errorlevel% equ 0 (
        echo [OK] MySQL service %%S is running
        set MYSQL_FOUND=1
        goto :mysql_ok
    )
)

:mysql_ok
if %MYSQL_FOUND% equ 0 (
    echo [INFO] MySQL not running, trying to start MySQL84...
    net start MySQL84 >nul 2>&1
    ping -n 6 127.0.0.1 > nul
    sc query MySQL84 2>nul | findstr "RUNNING" > nul 2>&1
    if %errorlevel% equ 0 (
        echo [OK] MySQL service started
    ) else (
        echo [ERROR] Cannot start MySQL. Please start it manually or run as Administrator.
        pause
        exit /b 1
    )
)

ping -n 3 127.0.0.1 > nul

REM 2. Setup MySQL user for JDBC
echo.
echo [2/7] Setting up database user...

where mysql >nul 2>&1
if %errorlevel% neq 0 (
    echo [INFO] mysql client not in PATH, skipping user setup
    goto :skip_mysql_setup
)

mysql -u root -p123456 -h 127.0.0.1 -e "ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '123456'; FLUSH PRIVILEGES;" 2>nul
if %errorlevel% neq 0 (
    echo [INFO] mysql_native_password not available, using caching_sha2_password
    mysql -u root -p123456 -h 127.0.0.1 -e "ALTER USER 'root'@'localhost' IDENTIFIED BY '123456'; FLUSH PRIVILEGES;" 2>nul
)
echo [OK] Database user configured
:skip_mysql_setup

REM 3. Kill existing processes on target ports
echo.
echo [3/7] Cleaning up existing processes...

taskkill /F /FI "WINDOWTITLE eq Backend*" > nul 2>&1
taskkill /F /FI "WINDOWTITLE eq Merchant Web*" > nul 2>&1
taskkill /F /FI "WINDOWTITLE eq Admin Web*" > nul 2>&1
ping -n 3 127.0.0.1 > nul

for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080 " ^| findstr "LISTENING"') do (
    echo [INFO] Killing process on port 8080 (PID: %%a)
    taskkill /F /PID %%a > nul 2>&1
)
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5173 " ^| findstr "LISTENING"') do (
    echo [INFO] Killing process on port 5173 (PID: %%a)
    taskkill /F /PID %%a > nul 2>&1
)
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5174 " ^| findstr "LISTENING"') do (
    echo [INFO] Killing process on port 5174 (PID: %%a)
    taskkill /F /PID %%a > nul 2>&1
)
ping -n 3 127.0.0.1 > nul

REM 4. Rebuild Backend JAR
echo.
echo [4/7] Rebuilding Backend JAR...
cd /d "%BACKEND_DIR%"

echo Running: %MVN% clean package -DskipTests
call %MVN% clean package -DskipTests
if %errorlevel% neq 0 (
    echo [ERROR] Maven build failed
    pause
    exit /b 1
)

if not exist "target\platform-api-0.0.1-SNAPSHOT.jar" (
    echo [ERROR] JAR file not found after build
    pause
    exit /b 1
)
echo [OK] JAR built successfully

REM 5. Start Backend (using start /b for background process within same console)
echo.
echo [5/7] Starting Backend Service...
cd /d "%BACKEND_DIR%"

set APP_AI_MOCK_ENABLED=true
set retries=0

:backend_retry
set /a retries+=1
echo [Attempt %retries%/%MAX_RETRIES%] Starting Backend...

REM Use start /b to run java in background within this console session
REM This avoids the quoting/window issues with "start cmd /k"
start /b "" "%JAVA_HOME%\bin\java.exe" -jar target\platform-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev

REM Wait for backend to start
echo [INFO] Waiting for backend to start (up to 30 seconds)...
set WAIT_COUNT=0
:wait_backend
set /a WAIT_COUNT+=1
ping -n 2 127.0.0.1 > nul
netstat -ano | findstr ":8080 " | findstr "LISTENING" > nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] Backend started on port 8080
    goto :backend_done
)
if %WAIT_COUNT% geq 15 (
    echo [INFO] 30 seconds elapsed, checking if process is still running...
    goto :backend_check
)
goto :wait_backend

:backend_check
REM Check if java process exists
tasklist /FI "IMAGENAME eq java.exe" 2>nul | findstr "java.exe" > nul 2>&1
if %errorlevel% equ 0 (
    echo [WARNING] Java process is running but port 8080 not yet listening. Waiting more...
    ping -n 10 127.0.0.1 > nul
    netstat -ano | findstr ":8080 " | findstr "LISTENING" > nul 2>&1
    if %errorlevel% equ 0 (
        echo [OK] Backend started on port 8080
        goto :backend_done
    )
)

if %retries% lss %MAX_RETRIES% (
    echo [WARNING] Backend not responding on attempt %retries%, retrying...
    taskkill /F /FI "IMAGENAME eq java.exe" > nul 2>&1
    ping -n %RETRY_DELAY% 127.0.0.1 > nul
    goto :backend_retry
)

echo [ERROR] Backend failed after %MAX_RETRIES% attempts
echo [INFO] Try running manually: cd /d %BACKEND_DIR% && set APP_AI_MOCK_ENABLED=true && "%JAVA_HOME%\bin\java.exe" -jar target\platform-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
pause
exit /b 1

:backend_done

REM 6. Start Merchant Frontend (port 5173)
echo.
echo [6/7] Starting Merchant Web...
cd /d "%MERCHANT_DIR%"

if not exist "node_modules" (
    echo [INFO] node_modules not found, running npm install...
    call npm install
    if %errorlevel% neq 0 (
        echo [ERROR] npm install failed for Merchant Web
        pause
        exit /b 1
    )
)

start "Merchant Web" cmd /k "cd /d %MERCHANT_DIR% && npm run dev"
ping -n 10 127.0.0.1 > nul

netstat -ano | findstr ":5173 " | findstr "LISTENING" > nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] Merchant Web started on port 5173
) else (
    echo [WARNING] Merchant Web may not have started properly - check the window
)

REM 7. Start Admin Frontend (port 5174)
echo.
echo [7/7] Starting Admin Web...
cd /d "%ADMIN_DIR%"

if not exist "node_modules" (
    echo [INFO] node_modules not found, running npm install...
    call npm install
    if %errorlevel% neq 0 (
        echo [ERROR] npm install failed for Admin Web
        pause
        exit /b 1
    )
)

start "Admin Web" cmd /k "cd /d %ADMIN_DIR% && npm run dev"
ping -n 10 127.0.0.1 > nul

netstat -ano | findstr ":5174 " | findstr "LISTENING" > nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] Admin Web started on port 5174
) else (
    echo [WARNING] Admin Web may not have started properly - check the window
)

ping -n 3 127.0.0.1 > nul

echo.
echo ========================================
echo All services started!
echo ========================================
echo.
echo Backend API:    http://localhost:8080
echo Backend Health: http://localhost:8080/actuator/health
echo Merchant Web:   http://localhost:5173
echo Admin Web:      http://localhost:5174
echo.
echo Test Accounts:
echo   Merchant: 13800000001 / 123456
echo.
echo NOTE: Backend runs in this console window.
echo       Closing this window will stop the backend.
echo       Frontend windows can be closed independently.
echo.
pause