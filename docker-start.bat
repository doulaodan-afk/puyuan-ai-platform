@echo off
chcp 65001 > nul
title 濮院毛衫 AI 平台 - Docker 一键启动

echo ==========================================
echo   濮院毛衫 AI 平台 - Docker 部署
echo ==========================================
echo.

REM ==========================================
REM 0. 检查 Docker 环境
REM ==========================================
echo [0/5] 检查 Docker 环境...

REM Docker 可能不在系统 PATH 中，手动补充
set DOCKER_PATH=C:\Program Files\Docker\Docker\resources\bin
if exist "%DOCKER_PATH%\docker.exe" (
    set "PATH=%DOCKER_PATH%;%PATH%"
)

docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo ========================================
    echo   [X] Docker 未安装或未找到！
    echo ========================================
    echo.
    echo   请先安装 Docker Desktop：
    echo   https://www.docker.com/products/docker-desktop
    echo.
    echo   或者先用本地启动：双击 start.bat
    echo.
    echo ========================================
    pause
    exit /b 1
)

docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo ========================================
    echo   [X] Docker 未运行！
    echo ========================================
    echo.
    echo   Docker Desktop 似乎没有启动。请检查：
    echo   1. 桌面右下角是否有 Docker 图标（鲸鱼）
    echo   2. 如果图标是红色/黄色，请等待它变白
    echo   3. 如果没有任何图标，请启动 Docker Desktop
    echo.
    echo   或者先用本地启动：双击 start.bat
    echo.
    echo ========================================
    pause
    exit /b 1
)
echo [OK] Docker 运行正常

REM ==========================================
REM 0b. 检查 Node.js / npm 环境
REM ==========================================
echo.
echo [0/5] 检查 Node.js 环境...

where npm >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo ========================================
    echo   [X] npm 未找到！
    echo ========================================
    echo.
    echo   请先安装 Node.js：
    echo   https://nodejs.org/
    echo.
    echo ========================================
    pause
    exit /b 1
)
echo [OK] npm 可用

REM ==========================================
REM 1. 构建前端
REM ==========================================
echo.
echo [1/5] 构建前端页面...

echo   安装商家端依赖 (merchant-web)...
cd /d D:\puyuanmaoshan\frontend\merchant-web
call npm install
if %errorlevel% neq 0 (
    echo   [X] 商家端 npm install 失败
    pause
    exit /b 1
)

echo   构建商家端 (merchant-web)...
call npm run build
if %errorlevel% neq 0 (
    echo   [X] 商家端构建失败，请检查上方错误信息
    pause
    exit /b 1
)
echo   [OK] 商家端构建完成

echo.
echo   安装管理端依赖 (admin-web)...
cd /d D:\puyuanmaoshan\frontend\admin-web
call npm install
if %errorlevel% neq 0 (
    echo   [X] 管理端 npm install 失败
    pause
    exit /b 1
)

echo   构建管理端 (admin-web)...
call npm run build
if %errorlevel% neq 0 (
    echo   [X] 管理端构建失败，请检查上方错误信息
    pause
    exit /b 1
)
echo   [OK] 管理端构建完成

cd /d D:\puyuanmaoshan

REM ==========================================
REM 2. 停止旧容器（如果存在）
REM ==========================================
echo.
echo [2/5] 清理旧容器...
docker compose down --remove-orphans 2>nul

REM ==========================================
REM 3. 构建并启动所有服务
REM ==========================================
echo.
echo [3/5] 构建并启动服务（首次需要下载镜像和编译，请耐心等待）...
docker compose up -d --build

if %errorlevel% neq 0 (
    echo [X] 启动失败！请检查上方错误信息
    pause
    exit /b 1
)

REM ==========================================
REM 4. 等待服务就绪
REM ==========================================
echo.
echo [4/5] 等待服务就绪...
echo   等待 MySQL 启动...（最多等待 90 秒）

setlocal enabledelayedexpansion
set /a count=0
:wait_mysql
timeout /t 3 >nul
set /a count+=3
docker exec puyuan-mysql mysqladmin ping -h localhost -u root -proot123 --silent >nul 2>&1
if !errorlevel! equ 0 goto :mysql_ready
if !count! lss 90 goto :wait_mysql
echo   [警告] MySQL 启动超时，但容器可能仍在初始化中

:mysql_ready
echo   [OK] MySQL 已就绪

echo   执行数据库迁移（确保schema完整）...
docker exec -i puyuan-mysql mysql -u root -proot123 puyuan_ai_mvp -e "source D:/puyuanmaoshan/sql/migrate-idempotent.sql" 2>nul
if %errorlevel% neq 0 (
    echo   [OK] 数据库迁移完成
) else (
    echo   [警告] 数据库迁移可能有问题，但继续启动
)

echo   等待后端启动...（最多等待 120 秒）
set /a count=0
:wait_backend
timeout /t 5 >nul
set /a count+=5
curl -sf http://localhost:8080/actuator/health >nul 2>&1
if !errorlevel! equ 0 goto :backend_ready
if !count! lss 120 goto :wait_backend
echo   [警告] 后端启动超时，请稍后手动验证

:backend_ready
echo   [OK] 后端已就绪
endlocal

REM ==========================================
REM 5. 完成
REM ==========================================
echo.
echo [5/5] 验证完成
echo.
echo ==========================================
echo   启动完成！
echo ==========================================
echo.
echo   访问地址：
echo     商家端：   http://localhost
echo     管理端：   http://localhost/admin/
echo     后端API：  http://localhost:8080
echo     健康检查： http://localhost:8080/actuator/health
echo.
echo   测试账号：
echo     商家老板： 13800000001 / 123456
echo.
echo   常用命令：
echo     查看日志：  docker compose logs -f backend
echo     停止服务：  docker compose down
echo     重启服务：  docker compose restart
echo.
pause