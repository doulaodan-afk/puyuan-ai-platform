#!/bin/bash
# ========================================
# 一键修复编码乱码脚本
# 在服务器上执行: ssh root@47.98.220.111 'bash -s' < fix_encoding_remote.sh
# 或直接在服务器上运行: bash fix_encoding_remote.sh
# ========================================
set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  濮院毛衫 AI 平台 - 编码乱码一键修复${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

PROJECT_DIR="/opt/puyuan-ai-platform"
if [ ! -d "$PROJECT_DIR" ]; then
    PROJECT_DIR="/root/puyuan-ai-platform"
fi
if [ ! -d "$PROJECT_DIR" ]; then
    PROJECT_DIR=$(dirname $(find / -name "docker-compose.yml" -path "*/puyuan*" 2>/dev/null | head -1))
fi

echo "项目目录: ${PROJECT_DIR}"

# ========================================
# Step 1: 获取 MySQL 连接信息
# ========================================
echo ""
echo -e "${YELLOW}[Step 1] 检测 MySQL 连接...${NC}"

if docker ps --format '{{.Names}}' 2>/dev/null | grep -q "puyuan-mysql"; then
    echo -e "${GREEN}检测到 Docker MySQL 容器${NC}"
    # 获取密码
    if [ -f "${PROJECT_DIR}/.env" ]; then
        source <(grep -E 'MYSQL_ROOT_PASSWORD|MYSQL_DATABASE' "${PROJECT_DIR}/.env" | sed 's/^/export /')
    fi
    MYSQL_PASS="${MYSQL_ROOT_PASSWORD}"
    MYSQL_DB="${MYSQL_DATABASE:-puyuan_ai_mvp}"
    MYSQL_CMD="docker exec -i puyuan-mysql mysql -uroot -p${MYSQL_PASS} ${MYSQL_DB}"

    # 检查是否能连接
    if ! docker exec puyuan-mysql mysqladmin ping -uroot -p"${MYSQL_PASS}" --silent 2>/dev/null; then
        echo -e "${RED}MySQL 连接失败，尝试查找密码...${NC}"
        # 尝试从 docker-compose 环境变量获取
        MYSQL_PASS=$(docker inspect puyuan-mysql 2>/dev/null | grep -o '"MYSQL_ROOT_PASSWORD=[^"]*"' | head -1 | cut -d= -f2 | tr -d '"')
        if [ -z "$MYSQL_PASS" ]; then
            MYSQL_PASS=$(docker inspect puyuan-mysql 2>/dev/null | grep -o '"MYSQL_ROOT_PASSWORD=[^,}]*"' | head -1 | sed 's/.*=//' | tr -d '"')
        fi
        MYSQL_CMD="docker exec -i puyuan-mysql mysql -uroot -p${MYSQL_PASS} ${MYSQL_DB}"
    fi
else
    echo -e "${YELLOW}未检测到 Docker，尝试直接连接 MySQL${NC}"
    MYSQL_CMD="mysql -uroot -p puyuan_ai_mvp"
fi

# ========================================
# Step 2: 检查当前字符集和乱码情况
# ========================================
echo ""
echo -e "${YELLOW}[Step 2] 检查数据库字符集配置...${NC}"

$MYSQL_CMD -e "
SELECT '=== 全局字符集 ===' AS ''; 
SHOW VARIABLES LIKE 'character_set%';
SELECT '=== 数据库字符集 ===' AS '';
SELECT SCHEMA_NAME, DEFAULT_CHARACTER_SET_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = '${MYSQL_DB}';
SELECT '=== 当前租户数据 ===' AS '';
SELECT id, tenant_code, name FROM tenant;
SELECT '=== 当前场景数据 ===' AS '';
SELECT id, scene_code, scene_name FROM ai_scene;
" 2>/dev/null

# ========================================
# Step 3: 修复数据库字符集
# ========================================
echo ""
echo -e "${YELLOW}[Step 3] 修复数据库字符集...${NC}"

$MYSQL_CMD <<SQL_EOF 2>/dev/null
-- 修改数据库字符集
ALTER DATABASE ${MYSQL_DB} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 修复所有表
ALTER TABLE tenant CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE ai_scene CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE ai_scene_model CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE ai_model CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE ai_provider CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE ai_model_provider CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE user_info CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE plugin_definition CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

SELECT 'Tables converted successfully' AS result;
SQL_EOF

echo -e "${GREEN}数据库和表字符集已修复${NC}"

# ========================================
# Step 4: 修复已损坏的中文数据
# ========================================
echo ""
echo -e "${YELLOW}[Step 4] 修复已损坏的中文数据（latin1->utf8mb4 逆转码）...${NC}"

$MYSQL_CMD <<SQL_EOF 2>/dev/null
-- 租户名称
UPDATE tenant SET name = CONVERT(BINARY CONVERT(name USING latin1) USING utf8mb4)
WHERE name != CONVERT(BINARY CONVERT(name USING latin1) USING utf8mb4);

-- 场景名称
UPDATE ai_scene SET scene_name = CONVERT(BINARY CONVERT(scene_name USING latin1) USING utf8mb4)
WHERE scene_name != CONVERT(BINARY CONVERT(scene_name USING latin1) USING utf8mb4);

-- 场景描述
UPDATE ai_scene SET scene_description = CONVERT(BINARY CONVERT(scene_description USING latin1) USING utf8mb4)
WHERE scene_description != CONVERT(BINARY CONVERT(scene_description USING latin1) USING utf8mb4);

-- 模型名称
UPDATE ai_model SET model_name = CONVERT(BINARY CONVERT(model_name USING latin1) USING utf8mb4)
WHERE model_name != CONVERT(BINARY CONVERT(model_name USING latin1) USING utf8mb4);

SELECT 'Data repair attempted' AS result;
SQL_EOF

echo -e "${GREEN}数据修复尝试完成${NC}"

# ========================================
# Step 5: 验证修复结果
# ========================================
echo ""
echo -e "${YELLOW}[Step 5] 验证修复结果...${NC}"

$MYSQL_CMD -e "
SELECT '=== 租户数据 ===' AS '';
SELECT id, tenant_code, name FROM tenant;
SELECT '=== 场景数据 ===' AS '';
SELECT id, scene_code, scene_name FROM ai_scene;
" 2>/dev/null

echo ""
echo -e "${YELLOW}请检查上面输出的中文是否正常显示。${NC}"
echo -e "${YELLOW}如果仍有乱码，说明数据在导入时已经损坏，需要重新导入。${NC}"

# ========================================
# Step 6: 重新构建和部署后端
# ========================================
echo ""
echo -e "${YELLOW}[Step 6] 重新构建后端（应用新的 JDBC 编码配置）...${NC}"

cd "${PROJECT_DIR}"

if [ -f "Dockerfile.backend" ]; then
    echo "检测到 Dockerfile.backend，重新构建镜像..."
    docker build -f Dockerfile.backend -t puyuan-api:latest .
fi

# 重启后端服务
if docker ps --format '{{.Names}}' 2>/dev/null | grep -q "puyuan-backend"; then
    echo "重启后端服务..."
    docker restart puyuan-backend
    echo "等待后端启动..."
    sleep 15
    echo -e "${GREEN}后端服务已重启${NC}"
elif docker ps --format '{{.Names}}' 2>/dev/null | grep -q "backend"; then
    echo "重启后端服务..."
    docker compose -f "${PROJECT_DIR}/deploy/docker-compose.yml" restart backend 2>/dev/null || \
    docker compose restart backend 2>/dev/null
    echo -e "${GREEN}后端服务已重启${NC}"
fi

# ========================================
# 完成
# ========================================
echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  修复完成！${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "请访问以下地址验证："
echo -e "  ${YELLOW}租户管理:${NC} https://ai.puyuanmaoshan.com/admin/tenants"
echo -e "  ${YELLOW}场景配置:${NC} https://ai.puyuanmaoshan.com/admin/ai-config/scenes"
echo ""
echo -e "${YELLOW}如果仍有乱码，请手动重新导入种子数据：${NC}"
echo "  docker exec -i puyuan-mysql mysql -uroot -p\${MYSQL_ROOT_PASSWORD} --default-character-set=utf8mb4 puyuan_ai_mvp < ${PROJECT_DIR}/sql/seed_mvp.sql"
echo ""
