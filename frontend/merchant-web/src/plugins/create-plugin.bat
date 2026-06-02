@echo off
setlocal enabledelayedexpansion

:: 插件脚手架初始化脚本
:: 用法: create-plugin.bat <plugin-id> <plugin-name>
:: 例:   create-plugin.bat my-cool-plugin "我的酷插件"

if "%~1"=="" (
    echo 用法: create-plugin.bat ^<plugin-id^> ^<plugin-name^>
    echo 例:   create-plugin.bat my-cool-plugin "我的酷插件"
    exit /b 1
)

set PLUGIN_ID=%~1
set PLUGIN_NAME=%~2
if "%PLUGIN_NAME%"=="" set PLUGIN_NAME=%PLUGIN_ID%

set SCRIPT_DIR=%~dp0
set TEMPLATE=%SCRIPT_DIR%_template
set TARGET=%SCRIPT_DIR%%PLUGIN_ID%

if exist "%TARGET%" (
    echo [ERROR] 插件目录已存在: %TARGET%
    exit /b 1
)

echo ==========================================
echo  创建插件: %PLUGIN_ID% (%PLUGIN_NAME%)
echo ==========================================

:: 复制模板目录
xcopy /E /I /Q "%TEMPLATE%" "%TARGET%"

:: 替换模板占位符
echo [1/2] 替换占位符...
for /r "%TARGET%" %%f in (*.ts *.vue *.json *.html) do (
    powershell -NoProfile -Command "$c = [IO.File]::ReadAllText('%%f', [Text.Encoding]::UTF8); $c = $c -replace '\{\{PLUGIN_ID\}\}', '%PLUGIN_ID%' -replace '\{\{PLUGIN_NAME\}\}', '%PLUGIN_NAME%'; [IO.File]::WriteAllText('%%f', $c, [Text.Encoding]::UTF8)"
)

:: 提示下一步
echo [2/2] 完成!
echo.
echo ==========================================
echo  插件已创建: %TARGET%
echo ==========================================
echo.
echo 下一步:
echo   1. 在 .env 中添加: VITE_DEV_PLUGINS=ai-design-assistant,%PLUGIN_ID%
echo   2. 在主框架中调试: cd frontend\merchant-web ^&^& npm run dev  (端口 5173)
echo   3. 独立调试: cd %TARGET% ^&^& npm install ^&^& npm run dev  (端口 5181)
echo   4. 打包上传: 修改 manifest.json 后打包为 ZIP
echo.

endlocal