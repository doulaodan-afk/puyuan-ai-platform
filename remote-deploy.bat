@echo off
chcp 65001 >nul 2>&1
setlocal

REM ============================================
REM 濮院毛衫 AI 平台 - 远程部署脚本
REM ============================================
REM 用法：
REM   remote-deploy.bat              完整更新（拉取代码 + 构建 + 重启）
REM   remote-deploy.bat status       查看服务器状态
REM   remote-deploy.bat restart      仅重启服务
REM   remote-deploy.bat logs         查看后端日志
REM   remote-deploy.bat ssh          直接 SSH 登录服务器
REM ============================================

set SSH_KEY=D:\puyuanmaoshan\ai_puyuan.pem
set SSH_USER=root
set SSH_HOST=47.98.220.111
set PROJECT_DIR=/opt/puyuan-ai-platform
set DEPLOY_DIR=%PROJECT_DIR%/deploy

REM 检查 PEM 密钥文件
if not exist "%SSH_KEY%" (
    echo [ERROR] SSH 密钥文件不存在: %SSH_KEY%
    echo 请确认 ai_puyuan.pem 文件在项目根目录下
    pause
    exit /b 1
)

REM 检查 ssh 命令
where ssh >nul 2>&1
if errorlevel 1 (
    echo [ERROR] ssh 命令未找到，请安装 OpenSSH
    echo Windows 10/11: 设置 → 应用 → 可选功能 → 添加 OpenSSH 客户端
    pause
    exit /b 1
)

set SSH_CMD=ssh -i "%SSH_KEY%" %SSH_USER%@%SSH_HOST%

REM 根据参数执行不同操作
if "%1"=="" goto deploy
if /i "%1"=="status" goto status
if /i "%1"=="restart" goto restart
if /i "%1"=="logs" goto logs
if /i "%1"=="ssh" goto ssh_login
goto usage

:deploy
echo.
echo ================================================
echo  濮院毛衫 AI 平台 - 远程部署（完整更新）
echo ================================================
echo.
echo [1/3] 推送本地代码到 GitHub...
git push origin main
if errorlevel 1 (
    echo [WARN] git push 失败，可能没有新提交或网络问题
    echo 继续尝试服务器端更新...
)
echo.
echo [2/3] 连接服务器执行版本迭代...
echo SSH: %SSH_USER%@%SSH_HOST%
echo.
%SSH_CMD% "cd %DEPLOY_DIR% && ./server-update.sh"
if errorlevel 1 (
    echo.
    echo [ERROR] 远程部署失败！
    echo 尝试手动登录排查: remote-deploy.bat ssh
    pause
    exit /b 1
)
echo.
echo [3/3] 部署完成！验证服务状态...
%SSH_CMD% "cd %DEPLOY_DIR% && ./server-update.sh --status"
echo.
echo ================================================
echo  部署成功！
echo  商户端:   https://ai.puyuanmaoshan.com/merchant/
echo  管理后台: https://ai.puyuanmaoshan.com/admin/
echo  API:      https://ai.puyuanmaoshan.com/api/
echo ================================================
pause
exit /b 0

:status
echo.
echo 查询服务器状态...
echo.
%SSH_CMD% "cd %DEPLOY_DIR% && ./server-update.sh --status"
pause
exit /b 0

:restart
echo.
echo 重启服务器服务...
echo.
%SSH_CMD% "cd %DEPLOY_DIR% && ./server-update.sh --restart"
echo.
echo 服务已重启
pause
exit /b 0

:logs
echo.
echo 查看后端日志（Ctrl+C 退出）...
echo.
%SSH_CMD% "cd %DEPLOY_DIR% && docker-compose logs -f --tail=100 backend"
pause
exit /b 0

:ssh_login
echo.
echo SSH 登录服务器（输入 exit 退出）...
echo.
%SSH_CMD%
exit /b 0

:usage
echo.
echo 用法: remote-deploy.bat [命令]
echo.
echo 命令:
echo   (无参数)   完整部署：git push + 服务器更新 + 状态检查
echo   status     查看服务器当前版本和服务状态
echo   restart    仅重启服务器上的 Docker 服务
echo   logs       查看后端实时日志
echo   ssh        直接 SSH 登录服务器
echo.
pause
exit /b 1
