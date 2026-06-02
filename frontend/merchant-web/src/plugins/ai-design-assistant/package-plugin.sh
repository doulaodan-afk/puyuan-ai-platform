#!/bin/bash
# AI 设计助手插件打包脚本
# 输出: ai-design-assistant.zip，解压后第一层直接是 manifest.json 和资源文件

set -e

PROJECT_ROOT="$(cd "$(dirname "$0")/../../../../.." && pwd)"
PLUGIN_NAME="ai-design-assistant"
VERSION="1.0.0"
OUTPUT_DIR="${PROJECT_ROOT}/release/plugins"

echo "=========================================="
echo " AI 设计助手插件打包"
echo "=========================================="
echo "项目根目录: ${PROJECT_ROOT}"
echo "插件名称: ${PLUGIN_NAME}"
echo "版本: ${VERSION}"
echo ""

# 创建输出目录
mkdir -p "${OUTPUT_DIR}"

# 清理之前的构建
echo "[1/4] 清理之前的构建..."
rm -rf "${PROJECT_ROOT}/temp_plugin"
rm -f "${OUTPUT_DIR}/${PLUGIN_NAME}.zip"

# 复制插件文件到临时目录（不嵌套额外目录层级）
echo "[2/4] 复制插件文件..."
mkdir -p "${PROJECT_ROOT}/temp_plugin"

# 复制前端插件资源（直接展开，不嵌套 ai-design-assistant 目录）
cp -r "${PROJECT_ROOT}/frontend/merchant-web/src/plugins/ai-design-assistant/"* "${PROJECT_ROOT}/temp_plugin/"

# 复制后端服务（如果存在）
BACKEND_SRC="${PROJECT_ROOT}/backend/java-spring/src/main/java/com/puyuanmaoshan/platform/plugin/ai_design_assistant"
if [ -d "${BACKEND_SRC}" ]; then
    echo "[3/4] 复制后端服务..."
    mkdir -p "${PROJECT_ROOT}/temp_plugin/backend"
    cp -r "${BACKEND_SRC}" "${PROJECT_ROOT}/temp_plugin/backend/"
else
    echo "[3/4] 跳过（后端代码不存在）"
fi

# 打包（解压后第一层就是 manifest.json 等文件）
echo "[4/4] 打包插件..."
cd "${PROJECT_ROOT}/temp_plugin"
zip -r "${OUTPUT_DIR}/${PLUGIN_NAME}.zip" ./*
cd "${PROJECT_ROOT}"

# 清理临时目录
rm -rf "${PROJECT_ROOT}/temp_plugin"

# 输出结果
ZIP_PATH="${OUTPUT_DIR}/${PLUGIN_NAME}.zip"
echo ""
echo "=========================================="
echo " 打包完成!"
echo " 输出: ${ZIP_PATH}"
echo "=========================================="
ls -lh "${ZIP_PATH}"

# 显示 ZIP 内容结构
echo ""
echo "ZIP 包内容结构:"
unzip -l "${ZIP_PATH" | head -30