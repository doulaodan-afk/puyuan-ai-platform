#!/bin/bash
set -e

PROJECT_DIR="/opt/puyuan-ai-platform"
GIT_REPO="https://github.com/doulaodan-afk/puyuan-ai-platform.git"
LOG_FILE="/var/log/puyuan-deploy.log"

exec > >(tee -a "$LOG_FILE") 2>&1

echo "========================================"
echo "濮院毛衫 AI 平台 - 部署开始"
echo "时间: $(date)"
echo "========================================"

# ----------------------------------------
# Step 1: 安装 Docker
# ----------------------------------------
echo ""
echo "[Step 1] 安装 Docker 和 Docker Compose..."

if command -v docker &>/dev/null; then
    echo "Docker 已安装: $(docker --version)"
else
    echo "安装 Docker 依赖..."
    apt-get update -qq
    apt-get install -y -qq ca-certificates curl gnupg lsb-release

    echo "添加 Docker GPG 密钥（阿里云镜像）..."
    install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://mirrors.aliyun.com/docker-ce/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
    chmod a+r /etc/apt/keyrings/docker.asc

    ARCH=$(dpkg --print-architecture)
    CODENAME=$(. /etc/os-release && echo "$VERSION_CODENAME")
    echo "架构: $ARCH, 代号: $CODENAME"
    echo "deb [arch=$ARCH signed-by=/etc/apt/keyrings/docker.asc] https://mirrors.aliyun.com/docker-ce/linux/ubuntu $CODENAME stable" > /etc/apt/sources.list.d/docker.list

    echo "安装 Docker..."
    apt-get update -qq
    apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

    systemctl enable docker
    systemctl start docker
    echo "Docker 安装完成: $(docker --version)"
fi

if docker compose version &>/dev/null; then
    echo "Docker Compose: $(docker compose version)"
fi

# 配置 Docker 镜像加速
echo "配置 Docker 镜像加速..."
mkdir -p /etc/docker
if [ ! -f /etc/docker/daemon.json ]; then
    cat > /etc/docker/daemon.json <<'EOF'
{
    "registry-mirrors": [
        "https://mirror.ccs.tencentyun.com",
        "https://docker.mirrors.ustc.edu.cn"
    ]
}
EOF
    systemctl daemon-reload
    systemctl restart docker
    echo "Docker 镜像加速已配置"
else
    echo "Docker 镜像加速已存在"
fi

# ----------------------------------------
# Step 2: 安装 Node.js
# ----------------------------------------
echo ""
echo "[Step 2] 安装 Node.js..."

if command -v node &>/dev/null && [ "$(node -v | cut -d. -f1 | tr -d v)" -ge 18 ]; then
    echo "Node.js 已安装: $(node --version)"
else
    echo "安装 Node.js 20 LTS..."
    curl -fsSL https://deb.nodesource.com/setup_20.x -o /tmp/nodesource_setup.sh
    bash /tmp/nodesource_setup.sh
    apt-get install -y nodejs
    echo "Node.js 安装完成: $(node --version), npm: $(npm --version)"
fi

# ----------------------------------------
# Step 3: 克隆项目代码
# ----------------------------------------
echo ""
echo "[Step 3] 获取项目代码..."

if [ -d "$PROJECT_DIR" ]; then
    echo "项目目录已存在，拉取最新代码..."
    cd "$PROJECT_DIR"
    git fetch origin
    git checkout main
    git reset --hard origin/main
else
    echo "克隆项目..."
    git clone "$GIT_REPO" "$PROJECT_DIR"
    cd "$PROJECT_DIR"
fi

echo "当前代码版本: $(git log --oneline -1)"

# ----------------------------------------
# Step 4: 构建后端 JAR
# ----------------------------------------
echo ""
echo "[Step 4] 构建后端..."

cd "$PROJECT_DIR/backend/java-spring"
if [ -f "target/platform-api-0.0.1-SNAPSHOT.jar" ]; then
    echo "后端 JAR 已存在，跳过构建"
else
    echo "Maven 构建（跳过测试）..."
    mvn clean package -DskipTests
    echo "后端构建完成: $(ls -lh target/*.jar)"
fi

# ----------------------------------------
# Step 5: 构建前端
# ----------------------------------------
echo ""
echo "[Step 5] 构建前端..."

echo "构建 merchant-web..."
cd "$PROJECT_DIR/frontend/merchant-web"
if [ -d "dist" ] && [ -n "$(ls -A dist 2>/dev/null)" ]; then
    echo "merchant-web dist 已存在，跳过构建"
else
    npm install --registry=https://registry.npmmirror.com
    npm run build
    echo "merchant-web 构建完成"
fi

echo "构建 admin-web..."
cd "$PROJECT_DIR/frontend/admin-web"
if [ -d "dist" ] && [ -n "$(ls -A dist 2>/dev/null)" ]; then
    echo "admin-web dist 已存在，跳过构建"
else
    npm install --registry=https://registry.npmmirror.com
    npm run build
    echo "admin-web 构建完成"
fi

# ----------------------------------------
# Step 6: 配置 Dockerfile
# ----------------------------------------
echo ""
echo "[Step 6] 配置后端 Dockerfile..."

cd "$PROJECT_DIR"
cat > backend/java-spring/Dockerfile <<'DOCKERFILE'
FROM openjdk:17-jdk-slim

WORKDIR /app

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

ARG JAR_FILE=target/platform-api-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:--Xms256m -Xmx512m} -jar app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-docker}"]
DOCKERFILE

echo "Dockerfile 已更新"

# ----------------------------------------
# Step 7: 创建 .env 配置
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
    echo ".env 文件已存在，跳过"
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

echo "等待服务启动（30秒）..."
sleep 30

# ----------------------------------------
# Step 9: 验证部署
# ----------------------------------------
echo ""
echo "[Step 9] 验证部署..."

echo "--- Docker 容器状态 ---"
docker compose ps

echo ""
echo "--- 端口监听 ---"
ss -tlnp | grep -E ':(80|3307|6379|8080) ' || echo "未检测到服务端口"

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

# ----------------------------------------
# 完成
# ----------------------------------------
echo ""
echo "========================================"
echo "部署完成！"
echo "========================================"
echo ""
echo "访问地址："
echo "  商家端: http://47.98.220.111/merchant/"
echo "  管理端: http://47.98.220.111/admin/"
echo "  后端API: http://47.98.220.111:8080"
echo "  Swagger: http://47.98.220.111:8080/swagger-ui.html"
echo ""
echo "管理命令："
echo "  查看日志: cd $PROJECT_DIR && docker compose logs -f"
echo "  重启服务: cd $PROJECT_DIR && docker compose restart"
echo "  停止服务: cd $PROJECT_DIR && docker compose down"
echo ""
