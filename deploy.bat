@echo off
REM ============================================================
REM 濮院毛衫 AI 平台 - 自动化部署脚本 (Windows)
REM 
REM 功能: git pull → 后端打包 → 前端打包 → 数据库迁移 → 重启服务 → 健康检查
REM 用法: deploy.bat [--skip-build] [--skip-frontend] [--skip-db] [--rollback]
REM ============================================================

setlocal enabledelayedexpansion

REM ==================== 配置 ====================
set PROJECT_DIR=%~dp0
set BACKEND_DIR=%PROJECT_DIR%backend\java-spring
set MERCHANT_WEB_DIR=%PROJECT_DIR%frontend\merchant-web
set ADMIN_WEB_DIR=%PROJECT_DIR%frontend\admin-web
set DEPLOY_DIR=%PROJECT_DIR%deploy
set JAR_FILE=%BACKEND_DIR%\target\platform-api-0.0.1-SNAPSHOT.jar
set HEALTH_CHECK_URL=http://localhost:8080/actuator/health
set MAX_HEALTH_RETRIES=30
set HEALTH_RETRY_INTERVAL=2

REM 生成备份目录名
for /f "tokens=1-4 delims=/- " %%a in ('date /t') do set DATESTR=%%a%%b%%c
for /f "tokens=1-2 delims=/: " %%a in ('time /t') do set TIMESTR=%%a%%b
set BACKUP_DIR=%DEPLOY_DIR%\backups\%DATESTR%_%TIMESTR%

REM ==================== 标志位 ====================
set SKIP_BUILD=false
set SKIP_FRONTEND=false
set SKIP_DB=false
set ROLLBACK=false

REM ==================== 参数解析 ====================
:parse_args
if "%~1"=="" goto :check_env
if "%~1"=="--skip-build" (
    set SKIP_BUILD=true
    shift
    goto :parse_args
)
if "%~1"=="--skip-frontend" (
    set SKIP_FRONTEND=true
    shift
    goto :parse_args
)
if "%~1"=="--skip-db" (
    set SKIP_DB=true
    shift
    goto :parse_args
)
if "%~1"=="--rollback" (
    set ROLLBACK=true
    shift
    goto :parse_args
)
if "%~1"=="-h" goto :usage
if "%~1"=="--help" goto :usage
echo [ERROR] 未知选项: %~1
goto :usage

:usage
echo 用法: deploy.bat [选项]
echo.
echo 选项:
echo   --skip-build      跳过 Maven 后端编译
echo   --skip-frontend   跳过前端构建
echo   --skip-db         跳过数据库迁移
echo   --rollback        回滚到上一个版本
echo   -h, --help        显示帮助信息
echo.
echo 示例:
echo   deploy.bat                      完整部署
echo   deploy.bat --skip-frontend      仅部署后端
echo   deploy.bat --skip-build         使用已有 jar 包部署
echo   deploy.bat --rollback           回滚到上一版本
exit /b 0

REM ==================== 回滚模式 ====================
if "%ROLLBACK%"=="true" goto :do_rollback

REM ==================== 打印头部 ====================
echo.
echo ================================================================
echo   濮院毛衫 AI 平台 - 自动化部署 (Windows)
echo   时间: %date% %time%
echo ================================================================
echo.

REM ==================== 1. 环境检查 ====================
:check_env
echo [STEP] 1. 环境检查
echo ----------------------------------------------------------------

REM 检查 Java
where java >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] 未找到 Java，请安装 JDK 17+
    exit /b 1
)
for /f "tokens=3" %%i in ('java -version 2^>^&1 ^| findstr /i "version"') do set JAVA_VER=%%i
echo [INFO] Java 版本: %JAVA_VER%

REM 检查 Maven
where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] 未找到 Maven，请安装 Maven 3.8+
    exit /b 1
)
echo [INFO] Maven 已安装

REM 检查 Node.js
where node >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] 未找到 Node.js，请安装 Node.js 18+
    exit /b 1
)
for /f "tokens=*" %%i in ('node -v') do set NODE_VER=%%i
echo [INFO] Node.js 版本: %NODE_VER%

REM 检查 npm
where npm >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] 未找到 npm
    exit /b 1
)
echo [INFO] npm 已安装

REM 检查 Git
where git >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] 未找到 Git
    exit /b 1
)
echo [INFO] Git 已安装

REM 检查 Docker
where docker >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARN] 未找到 Docker，将跳过 Docker 相关操作
)
echo [INFO] 环境检查通过
echo.

REM ==================== 2. 拉取最新代码 ====================
echo [STEP] 2. 拉取最新代码
echo ----------------------------------------------------------------
cd /d "%PROJECT_DIR%"

REM 检查是否有未提交的修改
git diff --quiet >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARN] 检测到未提交的本地修改
    set /p confirm="是否继续？未提交的修改将被暂存 (y/N): "
    if /i not "!confirm!"=="y" (
        echo [INFO] 部署已取消
        exit /b 0
    )
    git stash push -m "auto-stash-before-deploy-%date%"
)

echo [INFO] 正在拉取最新代码...
git fetch origin
git reset --hard origin/main
echo [INFO] 代码已更新
echo.

REM ==================== 3. 后端编译打包 ====================
if "%SKIP_BUILD%"=="true" goto :skip_backend
echo [STEP] 3. 后端编译打包
echo ----------------------------------------------------------------
cd /d "%BACKEND_DIR%"

echo [INFO] 正在清理旧的构建产物...
call mvn clean -q
if %errorlevel% neq 0 (
    echo [ERROR] Maven clean 失败
    exit /b 1
)

echo [INFO] 正在编译...
call mvn compile -q
if %errorlevel% neq 0 (
    echo [ERROR] 后端编译失败！
    exit /b 1
)

echo [INFO] 正在运行测试...
call mvn test -q
if %errorlevel% neq 0 (
    echo [ERROR] 后端测试未通过！
    echo [WARN] 如需跳过测试，请使用 --skip-build 然后手动打包
    exit /b 1
)

echo [INFO] 正在打包...
call mvn package -DskipTests -q
if %errorlevel% neq 0 (
    echo [ERROR] 后端打包失败！
    exit /b 1
)

if not exist "%JAR_FILE%" (
    echo [ERROR] Jar 文件未生成: %JAR_FILE%
    exit /b 1
)

echo [INFO] 后端打包完成
echo.

:skip_backend
if "%SKIP_BUILD%"=="true" (
    echo [WARN] 跳过后端编译 (--skip-build^)
    if not exist "%JAR_FILE%" (
        echo [ERROR] Jar 文件不存在且跳过了编译: %JAR_FILE%
        exit /b 1
    )
)

REM ==================== 4. 前端编译打包 ====================
if "%SKIP_FRONTEND%"=="true" goto :skip_frontend
echo [STEP] 4. 前端编译打包
echo ----------------------------------------------------------------

REM 商家端
echo [INFO] 正在构建商家端 (merchant-web^)...
cd /d "%MERCHANT_WEB_DIR%"
if not exist "node_modules" (
    echo [INFO] 安装商家端依赖...
    call npm ci --silent
    if %errorlevel% neq 0 (
        echo [ERROR] 商家端依赖安装失败
        exit /b 1
    )
)
call npm run build
if %errorlevel% neq 0 (
    echo [ERROR] 商家端构建失败！
    exit /b 1
)
echo [INFO] 商家端构建完成

REM 管理端
echo [INFO] 正在构建管理端 (admin-web^)...
cd /d "%ADMIN_WEB_DIR%"
if not exist "node_modules" (
    echo [INFO] 安装管理端依赖...
    call npm ci --silent
    if %errorlevel% neq 0 (
        echo [ERROR] 管理端依赖安装失败
        exit /b 1
    )
)
call npm run build
if %errorlevel% neq 0 (
    echo [ERROR] 管理端构建失败！
    exit /b 1
)
echo [INFO] 管理端构建完成
echo.

:skip_frontend
if "%SKIP_FRONTEND%"=="true" (
    echo [WARN] 跳过前端构建 (--skip-frontend^)
)

REM ==================== 5. 备份当前版本 ====================
echo [STEP] 5. 备份当前版本
echo ----------------------------------------------------------------
mkdir "%BACKUP_DIR%" 2>nul

if exist "%DEPLOY_DIR%\platform-api-0.0.1-SNAPSHOT.jar" (
    copy "%DEPLOY_DIR%\platform-api-0.0.1-SNAPSHOT.jar" "%BACKUP_DIR%\" >nul
    echo [INFO] 已备份 Jar 文件
)

if exist "%DEPLOY_DIR%\frontend\merchant-web\dist" (
    mkdir "%BACKUP_DIR%\frontend\merchant-web" 2>nul
    xcopy "%DEPLOY_DIR%\frontend\merchant-web\dist" "%BACKUP_DIR%\frontend\merchant-web\dist\" /E /I /Q >nul
    echo [INFO] 已备份商家端前端文件
)

if exist "%DEPLOY_DIR%\frontend\admin-web\dist" (
    mkdir "%BACKUP_DIR%\frontend\admin-web" 2>nul
    xcopy "%DEPLOY_DIR%\frontend\admin-web\dist" "%BACKUP_DIR%\frontend\admin-web\dist\" /E /I /Q >nul
    echo [INFO] 已备份管理端前端文件
)

echo [INFO] 备份完成: %BACKUP_DIR%
echo.

REM ==================== 6. 数据库迁移 ====================
if "%SKIP_DB%"=="true" goto :skip_db
echo [STEP] 6. 数据库迁移
echo ----------------------------------------------------------------
echo [INFO] 数据库迁移将在应用启动时由 Flyway 自动执行
echo [INFO] 如需手动迁移，请执行 sql 目录下的脚本
echo.

:skip_db
if "%SKIP_DB%"=="true" (
    echo [WARN] 跳过数据库迁移 (--skip-db^)
)

REM ==================== 7. 部署产物 ====================
echo [STEP] 7. 部署产物
echo ----------------------------------------------------------------

REM 复制 jar
echo [INFO] 正在部署后端 Jar...
copy /Y "%JAR_FILE%" "%DEPLOY_DIR%\platform-api-0.0.1-SNAPSHOT.jar" >nul
echo [INFO] Jar 文件已部署到 deploy\ 目录

REM 复制前端
if "%SKIP_FRONTEND%"=="false" (
    echo [INFO] 正在部署前端文件...
    mkdir "%DEPLOY_DIR%\frontend\merchant-web" 2>nul
    mkdir "%DEPLOY_DIR%\frontend\admin-web" 2>nul

    if exist "%DEPLOY_DIR%\frontend\merchant-web\dist" (
        rmdir /S /Q "%DEPLOY_DIR%\frontend\merchant-web\dist"
    )
    if exist "%DEPLOY_DIR%\frontend\admin-web\dist" (
        rmdir /S /Q "%DEPLOY_DIR%\frontend\admin-web\dist"
    )

    xcopy "%MERCHANT_WEB_DIR%\dist" "%DEPLOY_DIR%\frontend\merchant-web\dist\" /E /I /Q >nul
    xcopy "%ADMIN_WEB_DIR%\dist" "%DEPLOY_DIR%\frontend\admin-web\dist\" /E /I /Q >nul
    echo [INFO] 前端文件已部署到 deploy\ 目录
)
echo.

REM ==================== 8. 重启服务 ====================
echo [STEP] 8. 重启服务
echo ----------------------------------------------------------------
where docker >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARN] Docker 未安装，跳过服务重启
    echo [INFO] 请手动启动后端服务: java -jar %DEPLOY_DIR%\platform-api-0.0.1-SNAPSHOT.jar
    goto :health_check
)

cd /d "%DEPLOY_DIR%"
echo [INFO] 正在重启后端服务...
docker-compose restart backend 2>nul || docker-compose up -d backend
echo [INFO] 正在重启 Nginx...
docker-compose restart nginx 2>nul || docker-compose up -d nginx
echo [INFO] 服务重启完成
echo.

REM ==================== 9. 健康检查 ====================
:health_check
echo [STEP] 9. 健康检查
echo ----------------------------------------------------------------

set retry=0
:health_loop
set /a retry+=1
if %retry% gtr %MAX_HEALTH_RETRIES% (
    echo [ERROR] 后端服务健康检查超时！
    goto :end
)

curl -sf "%HEALTH_CHECK_URL%" >nul 2>&1
if %errorlevel% equ 0 (
    echo [INFO] 后端健康检查通过
    goto :health_ok
)

echo 等待服务启动... (%retry%/%MAX_HEALTH_RETRIES%^)
timeout /t %HEALTH_RETRY_INTERVAL% /nobreak >nul
goto :health_loop

:health_ok
echo [INFO] 健康检查完成
echo.

REM ==================== 部署总结 ====================
:end
echo ================================================================
echo   ✓  部署完成！
echo ================================================================
echo.
echo   项目目录:     %PROJECT_DIR%
echo   部署目录:     %DEPLOY_DIR%
echo   备份目录:     %BACKUP_DIR%
echo.
echo   后端健康检查:  %HEALTH_CHECK_URL%
echo   商家端:        http://localhost/merchant/
echo   管理端:        http://localhost/admin/
echo.
echo   常用命令:
echo     查看状态:    docker-compose -f deploy\docker-compose.yml ps
echo     查看日志:    docker-compose -f deploy\docker-compose.yml logs -f
echo     停止服务:    docker-compose -f deploy\docker-compose.yml down
echo.
pause
exit /b 0

REM ==================== 回滚 ====================
:do_rollback
echo [STEP] 回滚到上一个版本
echo ----------------------------------------------------------------

REM 查找最新的备份目录
set LATEST_BACKUP=
for /f "tokens=*" %%i in ('dir "%DEPLOY_DIR%\backups" /AD /B /O-D 2^>nul') do (
    if "!LATEST_BACKUP!"=="" set LATEST_BACKUP=%DEPLOY_DIR%\backups\%%i
)

if "%LATEST_BACKUP%"=="" (
    echo [ERROR] 没有找到备份文件，无法回滚
    exit /b 1
)

echo [INFO] 使用备份: %LATEST_BACKUP%

cd /d "%DEPLOY_DIR%"

REM 停止服务
echo [INFO] 正在停止服务...
docker-compose stop backend nginx 2>nul

REM 恢复 Jar
if exist "%LATEST_BACKUP%\platform-api-0.0.1-SNAPSHOT.jar" (
    copy /Y "%LATEST_BACKUP%\platform-api-0.0.1-SNAPSHOT.jar" "%DEPLOY_DIR%\" >nul
    echo [INFO] 已恢复 Jar 文件
)

REM 恢复前端
if exist "%LATEST_BACKUP%\frontend\merchant-web\dist" (
    rmdir /S /Q "%DEPLOY_DIR%\frontend\merchant-web\dist" 2>nul
    xcopy "%LATEST_BACKUP%\frontend\merchant-web\dist" "%DEPLOY_DIR%\frontend\merchant-web\dist\" /E /I /Q >nul
    echo [INFO] 已恢复商家端前端
)
if exist "%LATEST_BACKUP%\frontend\admin-web\dist" (
    rmdir /S /Q "%DEPLOY_DIR%\frontend\admin-web\dist" 2>nul
    xcopy "%LATEST_BACKUP%\frontend\admin-web\dist" "%DEPLOY_DIR%\frontend\admin-web\dist\" /E /I /Q >nul
    echo [INFO] 已恢复管理端前端
)

REM 启动服务
echo [INFO] 正在启动服务...
docker-compose start backend nginx 2>nul

echo [INFO] 回滚完成！
goto :end
