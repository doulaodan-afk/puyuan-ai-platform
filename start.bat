@echo off
chcp 65001 > nul
echo ========================================
echo Puyuan AI Platform - Start All Services
echo ========================================

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

REM 2. Start Backend
echo.
echo [2/4] Starting Backend Service...
cd /d D:\puyuanmaoshan\backend\java-spring
if exist target\platform-api-0.0.1-SNAPSHOT.jar (
    echo Using JAR file...
    start "Backend" cmd /k "java -jar target\platform-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev"
) else (
    echo JAR not found, using Maven...
    start "Backend" cmd /k "mvn spring-boot:run -Dspring-boot.run.profiles=dev"
)

timeout /t 10 > nul

REM 3. Start Merchant Frontend
echo.
echo [3/4] Starting Merchant Web...
cd /d D:\puyuanmaoshan\frontend\merchant-web
start "Merchant Web" cmd /k "npm run dev"

timeout /t 5 > nul

REM 4. Start Admin Frontend
echo.
echo [4/4] Starting Admin Web...
cd /d D:\puyuanmaoshan\frontend\admin-web
start "Admin Web" cmd /k "npm run dev"

timeout /t 3 > nul

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
