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

REM Set PATH
set PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%

REM 0. Verify Maven is available
echo.
echo [0/6] Verifying build environment...
where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARNING] Maven not in PATH, trying Maven wrapper...
    if exist "%MAVEN_HOME%\bin\mvn.cmd" (
        set MVNNAME=%MAVEN_HOME%\bin\mvn.cmd
    ) else (
        echo [ERROR] Maven not found. Please install Maven or add to PATH.
        exit /b 1
    )
) else (
    set MVNNAME=mvn
)

REM 1. Check MySQL is running
echo.
echo [1/6] Checking MySQL...
sc query MySQL84 | findstr "RUNNING" > nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] MySQL service is running
) else (
    echo [INFO] MySQL not running, trying to start...
    net start MySQL84 >nul 2>&1
    timeout /t 5 > nul
    sc query MySQL84 | findstr "RUNNING" > nul 2>&1
    if %errorlevel% equ 0 (
        echo [OK] MySQL service started
    ) else (
        echo [ERROR] Cannot start MySQL. Please start it manually or run as Administrator.
        exit /b 1
    )
)

timeout /t 2 > nul

REM 2. Setup MySQL user for JDBC (use 127.0.0.1 to avoid IPv6 auth issues)
echo.
echo [2/6] Setting up database user...
mysql -u root -p123456 -h 127.0.0.1 -e "ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '123456'; FLUSH PRIVILEGES;" 2>nul
if %errorlevel% neq 0 (
    echo [INFO] mysql_native_password not available, using caching_sha2_password instead
    mysql -u root -p123456 -h 127.0.0.1 -e "ALTER USER 'root'@'localhost' IDENTIFIED BY '123456'; FLUSH PRIVILEGES;" 2>nul
)

REM 3. Rebuild Backend JAR
echo.
echo [3/6] Rebuilding Backend JAR...
cd /d D:\puyuanmaoshan\backend\java-spring

REM Kill any existing backend process first
taskkill /F /FI "WINDOWTITLE eq Backend*" > nul 2>&1
timeout /t 2 > nul

REM Check and kill port 8080
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do (
    echo [WARNING] Killing process on port 8080 (PID: %%a)
    taskkill /F /PID %%a > nul 2>&1
)
timeout /t 2 > nul

REM Clean and rebuild
echo Running: %MVNNAME% clean package -DskipTests
call %MVNNAME% clean package -DskipTests
if %errorlevel% neq 0 (
    echo [ERROR] Maven build failed
    exit /b 1
)

if not exist "target\platform-api-0.0.1-SNAPSHOT.jar" (
    echo [ERROR] JAR file not found after build
    exit /b 1
)
echo [OK] JAR built successfully

REM 4. Start Backend
echo.
echo [4/6] Starting Backend Service...
cd /d D:\puyuanmaoshan\backend\java-spring

setlocal
set retries=0
:backend_retry
set /a retries+=1
echo [Attempt %retries%/%MAX_RETRIES%] Starting Backend...

start "Backend" cmd /k "set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot && set PATH=%JAVA_HOME%\bin;%PATH% && java -jar target\platform-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev"

REM Wait for backend to start
timeout /t 15 > nul

REM Check if port is listening
netstat -ano | findstr :8080 | findstr LISTENING > nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] Backend started on port 8080
    endlocal
    goto :backend_done
)

if %retries% lss %MAX_RETRIES% (
    echo [WARNING] Backend not responding, retrying...
    taskkill /F /FI "WINDOWTITLE eq Backend*" > nul 2>&1
    timeout /t %RETRY_DELAY% > nul
    goto :backend_retry
)

echo [ERROR] Backend failed after %MAX_RETRIES% attempts
endlocal
exit /b 1
:backend_done

REM 5. Start Merchant Frontend (port 5173)
echo.
echo [5/6] Starting Merchant Web...
cd /d D:\puyuanmaoshan\frontend\merchant-web

REM Kill any existing process
taskkill /F /FI "WINDOWTITLE eq Merchant Web*" > nul 2>&1
timeout /t 2 > nul

REM Check port 5173
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :5173 ^| findstr LISTENING') do (
    echo [WARNING] Killing process on port 5173 (PID: %%a)
    taskkill /F /PID %%a > nul 2>&1
)
timeout /t 2 > nul

start "Merchant Web" cmd /k "npm run dev"
timeout /t 5 > nul

netstat -ano | findstr :5173 | findstr LISTENING > nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] Merchant Web started on port 5173
) else (
    echo [WARNING] Merchant Web may not have started properly
)

REM 6. Start Admin Frontend (port 5174)
echo.
echo [6/6] Starting Admin Web...
cd /d D:\puyuanmaoshan\frontend\admin-web

REM Kill any existing process
taskkill /F /FI "WINDOWTITLE eq Admin Web*" > nul 2>&1
timeout /t 2 > nul

REM Check port 5174
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :5174 ^| findstr LISTENING') do (
    echo [WARNING] Killing process on port 5174 (PID: %%a)
    taskkill /F /PID %%a > nul 2>&1
)
timeout /t 2 > nul

start "Admin Web" cmd /k "npm run dev"
timeout /t 5 > nul

netstat -ano | findstr :5174 | findstr LISTENING > nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] Admin Web started on port 5174
) else (
    echo [WARNING] Admin Web may not have started properly
)

timeout /t 2 > nul

echo.
echo ========================================
echo All services started!
echo ========================================
echo.
echo Backend API:   http://localhost:8080
echo Backend Health: http://localhost:8080/actuator/health
echo Merchant Web:  http://localhost:5173
echo Admin Web:     http://localhost:5174
echo.
echo Test Accounts:
echo Merchant:      13800000001 / 123456
echo.
echo Press any key to close this window...
pause > nul