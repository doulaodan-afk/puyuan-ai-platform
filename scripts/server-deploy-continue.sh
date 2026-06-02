#!/bin/bash
set -e

PROJECT_DIR="/opt/puyuan-ai-platform"
LOG_FILE="/var/log/puyuan-deploy2.log"

# 确保 JDK 21 为默认
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-arm64

exec > >(tee -a "$LOG_FILE") 2>&1

echo "========================================"
echo "濮院毛衫 AI 平台 - 部署（续）"
echo "时间: $(date)"
echo "JDK: $(java -version 2>&1 | head -1)"
echo "========================================"

# ----------------------------------------
# Step 4: 构建后端 JAR
# ----------------------------------------
echo ""
echo "[Step 4] 构建后端..."

cd "$PROJECT_DIR/backend/java-spring"
rm -rf target/
echo "Maven 构建（跳过测试）..."
mvn clean package -DskipTests
echo "后端构建完成: $(ls -lh target/*.jar)"

# ----------------------------------------
# Step 5: 构建前端
# ----------------------------------------
echo ""
echo "[Step 5] 构建前端..."

echo "构建 merchant-web..."
cd "$PROJECT_DIR/frontend/merchant-web"
npm install --registry=https://registry.npmmirror.com
npm run build
echo "merchant-web 构建完成"

echo "构建 admin-web..."
cd "$PROJECT_DIR/frontend/admin-web"
npm install --registry=https://registry.npmmirror.com
npm run build
echo "admin-web 构建完成"

# ----------------------------------------
# Step 6: 配置 Dockerfile
# ----------------------------------------
echo ""
echo "[Step 6] 配置后端 Dockerfile..."

cd "$PROJECT_DIR"
cat > backend/java-spring/Dockerfile <<'DOCKERFILE'
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

RUN apk add --no-cache curl

ARG JAR_FILE=target/platform-api-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:--Xms256m -Xmx512m} -jar app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-docker}"]
DOCKERFILE

echo "Dockerfile 已更新（使用 JDK 21）"

# ----------------------------------------
# Step 7: 创建 .env
# ----------------------------------------
echo ""
echo "[Step 7] 创建环境配置..."

cd "$PROJECT_DIR"
if [ ! -f ".env" ]; then
    cat > .env <<'ENVFILE'
MYSQL_ROOT_PASSWORD=Puyuan2024!Root
MYSQL_DATABASE=puyuan_ai_mvp
MYSQL_USER=puyuan_user
MYSQL_PASSWORD=Puyuan2024!Db
ENVFILE
    echo ".env 文件已创建"
else
    echo ".env 文件已存在"
fi

# ----------------------------------------
# Step 8: 启动 Docker Compose
# ----------------------------------------
echo ""
echo "[Step 8] 启动服务..."

cd "$PROJECT_DIR"

echo "构建后端 Docker 镜像..."
docker compose build backend

echo "启动所有服务..."
docker compose up -d

echo "等待服务启动..."
sleep 30

# ----------------------------------------
# Step 9: 验证
# ----------------------------------------
echo ""
echo "[Step 9] 验证部署..."

echo "--- Docker 容器状态 ---"
docker compose ps

echo ""
echo "--- 端口监听 ---"
ss -tlnp | grep -E ':(80|3307|6379|8080) '

echo ""
echo "--- 后端健康检查 ---"
for i in $(seq 1 15); do
    HEALTH=$(curl -sf http://localhost:8080/actuator/health 2>/dev/null)
    if [ -n "$HEALTH" ]; then
        echo "后端服务正常: $HEALTH"
        break
    fi
    echo "等待后端启动... ($i/15)"
    sleep 10
done

echo ""
echo "--- Nginx 检查 ---"
curl -sf http://localhost/health && echo "Nginx 正常！" || echo "Nginx 未就绪"

echo ""
echo "========================================"
echo "部署完成！"
echo "========================================"
echo "商家端: http://47.98.220.111/merchant/"
echo "管理端: http://47.98.220.111/admin/"
echo "后端API: http://47.98.220.111:8080"
echo "Swagger: http://47.98.220.111:8080/swagger-ui.html"