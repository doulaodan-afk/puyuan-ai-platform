#!/bin/bash
# ========================================
# 修复数据库字符编码问题脚本
# 用于解决租户名称、场景名称等中文乱码
# 在服务器上执行: bash fix_encoding.sh
# ========================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  数据库字符编码修复脚本${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# ========================================
# Step 1: 检查是否在 Docker 环境中
# ========================================
echo -e "${YELLOW}[Step 1] 检测 MySQL 连接方式...${NC}"

# 尝试连接 Docker 容器中的 MySQL
if docker ps --format '{{.Names}}' | grep -q "puyuan-mysql"; then
    MYSQL_CMD="docker exec -i puyuan-mysql mysql -uroot -p\${MYSQL_ROOT_PASSWORD} puyuan_ai_mvp"
    echo -e "${GREEN}检测到 Docker 容器 puyuan-mysql${NC}"
    
    # 获取 root 密码
    if [ -f "${PROJECT_DIR}/.env" ]; then
        source "${PROJECT_DIR}/.env"
        MYSQL_PASS="${MYSQL_ROOT_PASSWORD:-root123}"
        MYSQL_CMD="docker exec -i puyuan-mysql mysql -uroot -p${MYSQL_PASS} puyuan_ai_mvp"
    elif [ -f "/opt/puyuan-ai-platform/.env" ]; then
        source "/opt/puyuan-ai-platform/.env"
        MYSQL_PASS="${MYSQL_ROOT_PASSWORD:-root123}"
        MYSQL_CMD="docker exec -i puyuan-mysql mysql -uroot -p${MYSQL_PASS} puyuan_ai_mvp"
    else
        MYSQL_CMD="docker exec -i puyuan-mysql mysql -uroot puyuan_ai_mvp"
    fi
else
    # 直接连接 MySQL（非 Docker）
    MYSQL_CMD="mysql -uroot -p puyuan_ai_mvp"
    echo -e "${YELLOW}未检测到 Docker 容器，尝试直接连接 MySQL${NC}"
fi

# ========================================
# Step 2: 检查数据库字符集配置
# ========================================
echo ""
echo -e "${YELLOW}[Step 2] 检查数据库字符集配置...${NC}"
echo "----------------------------------------"

echo "--- 全局字符集变量 ---"
$MYSQL_CMD -e "SHOW VARIABLES LIKE 'character_set%';" 2>/dev/null
echo ""

echo "--- 全局排序规则 ---"
$MYSQL_CMD -e "SHOW VARIABLES LIKE 'collation%';" 2>/dev/null
echo ""

echo "--- 数据库字符集 ---"
$MYSQL_CMD -e "SELECT SCHEMA_NAME, DEFAULT_CHARACTER_SET_NAME, DEFAULT_COLLATION_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = 'puyuan_ai_mvp';" 2>/dev/null
echo ""

# ========================================
# Step 3: 检查表字符集
# ========================================
echo -e "${YELLOW}[Step 3] 检查关键表字符集...${NC}"
echo "----------------------------------------"

TABLES=("tenant" "ai_scene" "ai_scene_model" "ai_model" "ai_provider")

for table in "${TABLES[@]}"; do
    echo "--- 表: $table ---"
    $MYSQL_CMD -e "SELECT TABLE_NAME, TABLE_COLLATION FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'puyuan_ai_mvp' AND TABLE_NAME = '$table';" 2>/dev/null
    echo ""
done

# ========================================
# Step 4: 检查当前数据是否有乱码
# ========================================
echo -e "${YELLOW}[Step 4] 检查当前数据...${NC}"
echo "----------------------------------------"

echo "--- 租户数据 ---"
$MYSQL_CMD -e "SELECT id, tenant_code, name, status FROM tenant ORDER BY id;" 2>/dev/null
echo ""

echo "--- 场景数据 ---"
$MYSQL_CMD -e "SELECT id, scene_code, scene_name, api_type FROM ai_scene ORDER BY id;" 2>/dev/null
echo ""

# ========================================
# Step 5: 修复数据库字符集
# ========================================
echo ""
echo -e "${YELLOW}[Step 5] 修复数据库默认字符集...${NC}"
echo "----------------------------------------"

$MYSQL_CMD <<EOF 2>/dev/null
-- 修改数据库默认字符集
ALTER DATABASE puyuan_ai_mvp CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
SELECT 'Database charset updated to utf8mb4' AS result;
EOF
echo -e "${GREEN}数据库默认字符集已修改为 utf8mb4${NC}"

# ========================================
# Step 6: 修复表字符集
# ========================================
echo ""
echo -e "${YELLOW}[Step 6] 修复关键表字符集...${NC}"
echo "----------------------------------------"

for table in "${TABLES[@]}"; do
    $MYSQL_CMD <<EOF 2>/dev/null
ALTER TABLE \`$table\` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
SELECT CONCAT('Table ', '$table', ' converted to utf8mb4') AS result;
EOF
    echo -e "${GREEN}表 $table 已转换为 utf8mb4${NC}"
done

# ========================================
# Step 7: 修复可能的乱码数据（双重编码修复）
# ========================================
echo ""
echo -e "${YELLOW}[Step 7] 尝试修复可能已损坏的中文数据...${NC}"
echo "----------------------------------------"
echo -e "${YELLOW}（使用 latin1 -> utf8mb4 转换尝试恢复被错误编码的数据）${NC}"

$MYSQL_CMD <<EOF 2>/dev/null
-- 修复租户名称乱码（尝试 latin1->utf8mb4 逆转码）
UPDATE tenant 
SET name = CONVERT(BINARY CONVERT(name USING latin1) USING utf8mb4)
WHERE HEX(name) REGEXP '^(..)+$' 
  AND name != CONVERT(BINARY CONVERT(name USING latin1) USING utf8mb4);
-- 注：如果数据本来就没问题，UPDATE 不会改变任何行

-- 修复场景名称乱码
UPDATE ai_scene 
SET scene_name = CONVERT(BINARY CONVERT(scene_name USING latin1) USING utf8mb4)
WHERE HEX(scene_name) REGEXP '^(..)+$' 
  AND scene_name != CONVERT(BINARY CONVERT(scene_name USING latin1) USING utf8mb4);

UPDATE ai_scene 
SET scene_description = CONVERT(BINARY CONVERT(scene_description USING latin1) USING utf8mb4)
WHERE HEX(scene_description) REGEXP '^(..)+$' 
  AND scene_description != CONVERT(BINARY CONVERT(scene_description USING latin1) USING utf8mb4);

-- 修复 AI 模型名称
UPDATE ai_model 
SET model_name = CONVERT(BINARY CONVERT(model_name USING latin1) USING utf8mb4)
WHERE HEX(model_name) REGEXP '^(..)+$' 
  AND model_name != CONVERT(BINARY CONVERT(model_name USING latin1) USING utf8mb4);
EOF

echo -e "${GREEN}数据修复尝试完成${NC}"

# ========================================
# Step 8: 重新验证
# ========================================
echo ""
echo -e "${YELLOW}[Step 8] 修复后验证数据...${NC}"
echo "----------------------------------------"

echo "--- 租户数据（修复后）---"
$MYSQL_CMD -e "SELECT id, tenant_code, name, status FROM tenant ORDER BY id;" 2>/dev/null
echo ""

echo "--- 场景数据（修复后）---"
$MYSQL_CMD -e "SELECT id, scene_code, scene_name FROM ai_scene ORDER BY id;" 2>/dev/null
echo ""

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  编码修复完成！${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${YELLOW}接下来请：${NC}"
echo "1. 重启后端服务使 JDBC 连接配置生效"
echo "2. 访问 https://ai.puyuanmaoshan.com/admin/tenants 验证租户名称"
echo "3. 访问 https://ai.puyuanmaoshan.com/admin/ai-config/scenes 验证场景名称"
echo ""
echo -e "${YELLOW}如果仍有乱码，可能需要重新导入 SQL 数据：${NC}"
echo "   mysql -uroot -p --default-character-set=utf8mb4 puyuan_ai_mvp < sql/seed_mvp.sql"
echo ""
