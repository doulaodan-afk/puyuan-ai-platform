@echo off
setlocal enabledelayedexpansion
chcp 65001 > nul
title 濮院毛衫 AI 平台 - 一键启动

echo ========================================
echo   濮院毛衫 AI 平台 - 本地开发一键启动
echo ========================================
echo.

REM ==================== 配置 ====================
set "PROJECT_DIR=%~dp0"
if "%PROJECT_DIR:~-1%"=="\" set "PROJECT_DIR=%PROJECT_DIR:~0,-1%"

set "BACKEND_DIR=%PROJECT_DIR%\backend\java-spring"
set "MERCHANT_DIR=%PROJECT_DIR%\frontend\merchant-web"
set "ADMIN_DIR=%PROJECT_DIR%\frontend\admin-web"

REM ==================== 0. 验证构建环境 ====================
echo [0/7] 验证构建环境...

REM 检查 Java（优先使用系统 JAVA_HOME，兜底用 PATH）
if defined JAVA_HOME (
    set "JAVA_BIN=%JAVA_HOME%\bin\java.exe"
    if exist "!JAVA_BIN!" (
        set "PATH=%JAVA_HOME%\bin;%PATH%"
        echo [OK] JAVA_HOME: %JAVA_HOME%
    ) else (
        echo [WARN] JAVA_HOME 已设置但 java.exe 不存在，尝试 PATH
    )
)

where java >nul 2>&1
if !errorlevel! neq 0 (
    echo.
    echo ========================================
    echo   [ERROR] 未找到 Java！
    echo ========================================
    echo.
    echo   请安装 JDK 17 并设置 JAVA_HOME 环境变量：
    echo   JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.xxx-hotspot
    echo.
    pause
    exit /b 1
)
for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do echo [OK] Java: %%v

REM 检查 Maven
if defined MAVEN_HOME (
    set "MAVEN_BIN=%MAVEN_HOME%\bin\mvn.cmd"
    if exist "!MAVEN_BIN!" (
        set "PATH=%MAVEN_HOME%\bin;%PATH%"
        echo [OK] MAVEN_HOME: %MAVEN_HOME%
    ) else (
        echo [WARN] MAVEN_HOME 已设置但 mvn.cmd 不存在，尝试 PATH
    )
)

where mvn >nul 2>&1
if !errorlevel! neq 0 (
    echo.
    echo ========================================
    echo   [ERROR] 未找到 Maven！
    echo ========================================
    echo.
    echo   请安装 Maven 并设置 MAVEN_HOME 环境变量。
    echo.
    pause
    exit /b 1
)
echo [OK] Maven 可用

REM ==================== 1. 检查 MySQL ====================
echo.
echo [1/7] 检查 MySQL...

set "MYSQL_OK=0"
for %%S in (MySQL84 MySQL80 MySQL MySQL57) do (
    sc query %%S 2>nul | findstr "RUNNING" >nul 2>&1
    if !errorlevel! equ 0 (
        echo [OK] MySQL 服务 "%%S" 正在运行
        set "MYSQL_OK=1"
    )
)

if "!MYSQL_OK!"=="0" (
    echo [WARN] MySQL 服务未运行，尝试启动 MySQL84...
    net start MySQL84 >nul 2>&1
    if !errorlevel! neq 0 (
        echo [WARN] 无法启动 MySQL，请手动启动后重试
        echo [INFO] 仍继续执行，后端启动时会再次检查...
    ) else (
        timeout /t 6 /nobreak >nul
        echo [OK] MySQL 已启动
    )
)

REM ==================== 2. 验证数据库连接 ====================
echo.
echo [2/7] 验证数据库连接...

where mysql >nul 2>&1
if !errorlevel! neq 0 (
    echo [INFO] mysql 客户端不在 PATH 中，跳过连接验证
    goto :skip_mysql_verify
)

mysql -u root -p123456 -h 127.0.0.1 -e "SELECT 1" 2>nul
if !errorlevel! equ 0 (
    echo [OK] 数据库连接验证成功 (root@127.0.0.1:3306)
) else (
    echo [WARN] 无法连接 MySQL (root/123456@127.0.0.1)
    echo [INFO] 后端启动时会自动尝试连接，继续执行...
)

:skip_mysql_verify

REM ==================== 3. 清理端口 ====================
echo.
echo [3/7] 清理端口 8080, 5173, 5174...

call :kill_port 8080
call :kill_port 5173
call :kill_port 5174
timeout /t 2 /nobreak >nul
echo [OK] 端口已清理

REM ==================== 4. 构建后端 ====================
echo.
echo [4/7] 构建后端 JAR 包...
cd /d "%BACKEND_DIR%"

echo [INFO] 执行 Maven 编译（首次可能较慢）...
call mvn clean package -DskipTests
if !errorlevel! neq 0 (
    echo.
    echo ========================================
    echo   [ERROR] Maven 编译失败！
    echo ========================================
    echo.
    echo   请查看上方错误信息，常见原因：
    echo   1. JDK 版本不匹配（需要 JDK 17）
    echo   2. 依赖下载失败（检查网络）
    echo   3. 代码编译错误
    echo.
    pause
    exit /b 1
)

if not exist "target\platform-api-0.0.1-SNAPSHOT.jar" (
    echo [ERROR] JAR 文件未生成，编译可能有问题
    pause
    exit /b 1
)
echo [OK] JAR 构建成功

REM ==================== 5. 启动后端 ====================
echo.
echo [5/7] 启动后端服务...

REM 重要：不使用 set APP_AI_MOCK_ENABLED=true 环境变量方式
REM 原因：cmd.exe 的 set 命令可能产生尾部空格，导致 Spring Boot
REM 的 @ConditionalOnProperty 将 "true " 误判为非 "true"
REM 使用 YAML 配置文件中的 app.ai.mock-enabled: true 即可

start "濮院后端 API" cmd /k "cd /d %BACKEND_DIR% && java -jar target\platform-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev"

REM 等待后端启动（最多 90 秒）
echo [INFO] 等待后端启动（最多 90 秒）...
set "backend_wait=0"
set "backend_ok=0"

:wait_backend_loop
timeout /t 3 /nobreak >nul
set /a backend_wait+=3

REM 先检查端口是否在监听
netstat -ano | findstr ":8080" | findstr "LISTENING" >nul 2>&1
if !errorlevel! neq 0 (
    if !backend_wait! lss 90 goto :wait_backend_loop
    echo [WARN] 后端在 90 秒内未监听 8080 端口
    goto :backend_check_done
)

REM 端口已监听，再检查 health endpoint
powershell -Command "try { Invoke-WebRequest -Uri 'http://localhost:8080/actuator/health' -TimeoutSec 3 -UseBasicParsing | Out-Null; exit 0 } catch { exit 1 }" >nul 2>&1
if !errorlevel! equ 0 (
    echo [OK] 后端启动成功，健康检查通过 (http://localhost:8080)
    set "backend_ok=1"
    goto :backend_check_done
)

REM 端口在监听但 health 还没就绪，继续等待
if !backend_wait! lss 90 goto :wait_backend_loop

:backend_check_done

if "!backend_ok!"=="0" (
    echo.
    echo [WARN] 后端可能未完全就绪，请查看"濮院后端 API"窗口
    echo [INFO] 如果窗口显示错误，常见原因：
    echo   1. MySQL 未运行或密码不对（需要 root/123456@127.0.0.1:3306）
    echo   2. 数据库 puyuan_ai_mvp 未创建
    echo.
    echo 手动启动命令：
    echo   cd /d %BACKEND_DIR%
    echo   java -jar target\platform-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
)

REM ==================== 6. 启动商家端 ====================
echo.
echo [6/7] 启动商家端前端 (端口 5173)...
cd /d "%MERCHANT_DIR%"

if not exist "node_modules" (
    echo [INFO] 安装商家端依赖...
    call npm install
    if !errorlevel! neq 0 (
        echo [ERROR] 商家端 npm install 失败
        pause
        exit /b 1
    )
)

start "商家端 Merchant Web" cmd /k "cd /d %MERCHANT_DIR% && npm run dev"
timeout /t 8 /nobreak >nul

netstat -ano | findstr ":5173" | findstr "LISTENING" >nul 2>&1
if !errorlevel! equ 0 (
    echo [OK] 商家端已启动 http://localhost:5173/merchant/
) else (
    echo [WARN] 商家端可能未启动 - 请检查其窗口
)

REM ==================== 7. 启动管理端 ====================
echo.
echo [7/7] 启动管理端前端 (端口 5174)...
cd /d "%ADMIN_DIR%"

if not exist "node_modules" (
    echo [INFO] 安装管理端依赖...
    call npm install
    if !errorlevel! neq 0 (
        echo [ERROR] 管理端 npm install 失败
        pause
        exit /b 1
    )
)

start "管理端 Admin Web" cmd /k "cd /d %ADMIN_DIR% && npm run dev"
timeout /t 8 /nobreak >nul

netstat -ano | findstr ":5174" | findstr "LISTENING" >nul 2>&1
if !errorlevel! equ 0 (
    echo [OK] 管理端已启动 http://localhost:5174/admin/
) else (
    echo [WARN] 管理端可能未启动 - 请检查其窗口
)

REM ==================== 总结 ====================
echo.
echo ========================================
echo   所有服务已启动！
echo ========================================
echo.
echo   后端 API:    http://localhost:8080
echo   健康检查:    http://localhost:8080/actuator/health
echo   商家端:      http://localhost:5173/merchant/
echo   管理端:      http://localhost:5174/admin/
echo.
echo   测试账号：
echo     商家老板: 13800000001 / 123456
echo.
echo   注意：
echo   - 后端在"濮院后端 API"窗口运行，关闭它即停止后端
echo   - 开发模式下 AI/短信/支付 均为 Mock 模拟，无需真实凭证
echo   - Redis 在开发模式下已禁用，无需启动
echo.
pause
exit /b 0

REM ==================== 辅助函数: kill_port ====================
:kill_port
setlocal
set "port=%~1"
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%port%" ^| findstr "LISTENING"') do (
    echo [INFO] 终止端口 %port% 上的进程 (PID: %%a)
    taskkill /F /PID %%a >nul 2>&1
)
endlocal
exit /b 0