@echo off
chcp 65001 > nul

echo ========================================
echo Puyuan AI Platform - Start All Services
echo ========================================

REM Configuration
set MAX_RETRIES=3
set RETRY_DELAY=5

REM 1. Start MySQL
echo.
echo [1/4] Starting MySQL...
net start MySQL84 2>nul
if %errorlevel% neq 0 (
    echo MySQL start failed, checking service...
    sc query state= all | findstr /i "mysql"
) else (
    echo MySQL started successfully
)

timeout /t 3 > nul

REM 2. Start Backend (port 8080)
echo.
echo [2/4] Starting Backend Service...
call :start_backend
goto :backend_done

:start_backend
REM Kill any existing Backend process first
taskkill /F /FI "WINDOWTITLE eq Backend*" > nul 2>&1
timeout /t 2 > nul

REM Check and kill port 8080
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do (
    echo [WARNING] Killing process on port 8080 (PID: %%a)
    taskkill /F /PID %%a > nul 2>&1
)
timeout /t 2 > nul

REM Try to start with retries
setlocal
set retries=0
:backend_retry
set /a retries+=1
echo [Attempt %retries%/%MAX_RETRIES%] Starting Backend...

cd /d D:\puyuanmaoshan\backend\java-spring
if exist target\platform-api-0.0.1-SNAPSHOT.jar (
    start "Backend" cmd /k "java -jar target\platform-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev"
) else (
    start "Backend" cmd /k "mvn spring-boot:run -Dspring-boot.run.profiles=dev"
)

REM Wait for backend to start
timeout /t 8 > nul

REM Check if port is listening
netstat -ano | findstr :8080 | findstr LISTENING > nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] Backend started on port 8080
    endlocal
    exit /b 0
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

REM 3. Start Merchant Frontend (port 5173)
echo.
echo [3/4] Starting Merchant Web...
call :start_merchant
goto :merchant_done

:start_merchant
REM Kill any existing process first
taskkill /F /FI "WINDOWTITLE eq Merchant Web*" > nul 2>&1
timeout /t 2 > nul

REM Check and kill port 5173
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :5173 ^| findstr LISTENING') do (
    echo [WARNING] Killing process on port 5173 (PID: %%a)
    taskkill /F /PID %%a > nul 2>&1
)
timeout /t 2 > nul

setlocal
set retries=0
:merchant_retry
set /a retries+=1
echo [Attempt %retries%/%MAX_RETRIES%] Starting Merchant Web...

cd /d D:\puyuanmaoshan\frontend\merchant-web
start "Merchant Web" cmd /k "npm run dev"

timeout /t 5 > nul

REM Check if port is listening
netstat -ano | findstr :5173 | findstr LISTENING > nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] Merchant Web started on port 5173
    endlocal
    exit /b 0
)

if %retries% lss %MAX_RETRIES% (
    echo [WARNING] Merchant Web not responding, retrying...
    taskkill /F /FI "WINDOWTITLE eq Merchant Web*" > nul 2>&1
    timeout /t %RETRY_DELAY% > nul
    goto :merchant_retry
)

echo [ERROR] Merchant Web failed after %MAX_RETRIES% attempts
endlocal
exit /b 1
:merchant_done

REM 4. Start Admin Frontend (port 5174)
echo.
echo [4/4] Starting Admin Web...
call :start_admin
goto :admin_done

:start_admin
REM Kill any existing process first
taskkill /F /FI "WINDOWTITLE eq Admin Web*" > nul 2>&1
timeout /t 2 > nul

REM Check and kill port 5174
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :5174 ^| findstr LISTENING') do (
    echo [WARNING] Killing process on port 5174 (PID: %%a)
    taskkill /F /PID %%a > nul 2>&1
)
timeout /t 2 > nul

setlocal
set retries=0
:admin_retry
set /a retries+=1
echo [Attempt %retries%/%MAX_RETRIES%] Starting Admin Web...

cd /d D:\puyuanmaoshan\frontend\admin-web
start "Admin Web" cmd /k "npm run dev"

timeout /t 5 > nul

REM Check if port is listening
netstat -ano | findstr :5174 | findstr LISTENING > nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] Admin Web started on port 5174
    endlocal
    exit /b 0
)

if %retries% lss %MAX_RETRIES% (
    echo [WARNING] Admin Web not responding, retrying...
    taskkill /F /FI "WINDOWTITLE eq Admin Web*" > nul 2>&1
    timeout /t %RETRY_DELAY% > nul
    goto :admin_retry
)

echo [ERROR] Admin Web failed after %MAX_RETRIES% attempts
endlocal
exit /b 1
:admin_done

timeout /t 2 > nul

echo.
echo ========================================
echo All services started!
echo ========================================
echo.
echo Backend API:   http://localhost:8080
echo Merchant Web:  http://localhost:5173
echo Admin Web:     http://localhost:5174
echo.
echo Test Accounts:
echo Merchant:      13800000001 / 123456
echo.
echo Press any key to close this window...
pause > nul