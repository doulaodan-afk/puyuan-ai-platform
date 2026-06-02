#!/bin/bash
set -e

echo "================================"
echo "濮院毛衫 AI 平台 - 生产环境部署"
echo "================================"
echo ""

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 1. 检查系统
echo -e "${YELLOW}[1/6] 检查系统环境...${NC}"
if ! command -v apt-get &> /dev/null; then
    echo -e "${RED}❌ 仅支持 Ubuntu/Debian 系统${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Ubuntu/Debian 系统${NC}"

# 2. 安装 Docker
echo ""
echo -e "${YELLOW}[2/6] 安装 Docker...${NC}"
if ! command -v docker &> /dev/null; then
    echo "Docker 未安装，开始安装..."
    sudo apt-get update
    sudo apt-get install -y ca-certificates curl gnupg lsb-release

    # 添加 Docker 官方 GPG 密钥
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg

    # 设置 Docker 仓库
    echo "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | \
        sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

    sudo apt-get update
    sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
    sudo systemctl start docker
    sudo systemctl enable docker
    echo -e "${GREEN}✓ Docker 安装完成${NC}"
else
    echo -e "${GREEN}✓ Docker 已安装 ($(docker --version))${NC}"
fi

# 3. 检查 Docker Compose
echo ""
echo -e "${YELLOW}[3/6] 检查 Docker Compose...${NC}"
if ! command -v docker-compose &> /dev/null; then
    if docker compose version &> /dev/null; then
        echo -e "${GREEN}✓ Docker Compose (V2) 已安装${NC}"
        DOCKER_COMPOSE="docker compose"
    else
        echo "Docker Compose 未安装，开始安装..."
        sudo apt-get install -y docker-compose
        echo -e "${GREEN}✓ Docker Compose 安装完成${NC}"
        DOCKER_COMPOSE="docker-compose"
    fi
else
    echo -e "${GREEN}✓ Docker Compose 已安装 ($(docker-compose --version))${NC}"
    DOCKER_COMPOSE="docker-compose"
fi

# 4. 检查 Java
echo ""
echo -e "${YELLOW}[4/6] 检查 Java 环境...${NC}"
if ! command -v java &> /dev/null; then
    echo "Java 未安装，开始安装..."
    sudo apt-get install -y openjdk-17-jdk
    echo -e "${GREEN}✓ Java 17 安装完成${NC}"
else
    echo -e "${GREEN}✓ Java 已安装 ($(java -version 2>&1 | head -n 1))${NC}"
fi

# 5. 构建后端应用
echo ""
echo -e "${YELLOW}[5/6] 构建后端应用...${NC}"
cd backend/java-spring
if [ ! -f "Dockerfile" ]; then
    echo -e "${RED}❌ 缺少 Dockerfile，正在创建...${NC}"
    cat > Dockerfile << 'EOF'
FROM openjdk:17-jdk-slim
WORKDIR /app
ARG JAR_FILE=target/platform-api-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF
fi

mvn clean package -Pprod -DskipTests
docker build -t puyuan-api:latest .
echo -e "${GREEN}✓ 后端应用构建完成${NC}"
cd ../..

# 6. 启动服务
echo ""
echo -e "${YELLOW}[6/6] 启动服务...${NC}"

# 创建 .env 文件
cat > .env.prod << 'EOF'
MYSQL_ROOT_PASSWORD=prod_root_password_123
MYSQL_PASSWORD=prod_mysql_password_123
MYSQL_DATABASE=puyuan_ai_mvp
MYSQL_USER=puyuan_user
REDIS_PASSWORD=prod_redis_password_123
SPRING_PROFILES_ACTIVE=prod
EOF

echo "⚠️  请编辑 .env.prod 文件，更改数据库密码！"
echo "编辑完成后，继续部署..."
read -p "按 Enter 继续..."

# 启动容器
$DOCKER_COMPOSE up -d --env-file .env.prod

# 等待服务启动
echo "等待服务启动..."
sleep 10

# 检查服务状态
echo ""
echo -e "${YELLOW}检查服务状态...${NC}"
if $DOCKER_COMPOSE ps | grep -q "puyuan-mysql"; then
    echo -e "${GREEN}✓ MySQL 容器运行中${NC}"
fi
if $DOCKER_COMPOSE ps | grep -q "puyuan-redis"; then
    echo -e "${GREEN}✓ Redis 容器运行中${NC}"
fi
if $DOCKER_COMPOSE ps | grep -q "puyuan-backend"; then
    echo -e "${GREEN}✓ 后端应用运行中${NC}"
fi

# 部署前端
echo ""
echo -e "${YELLOW}部署前端应用...${NC}"
cd frontend/merchant-web
npm run build
cd ..

cd admin-web
npm run build
cd ../..

echo -e "${GREEN}✓ 前端应用构建完成${NC}"

echo ""
echo "================================"
echo -e "${GREEN}✅ 部署完成！${NC}"
echo "================================"
echo ""
echo "📍 服务地址："
echo "   后端 API: http://your-server-ip:8080"
echo "   商家端: http://your-server-ip/merchant"
echo "   管理端: http://your-server-ip/admin"
echo ""
echo "🔧 常用命令："
echo "   查看日志: $DOCKER_COMPOSE logs -f backend"
echo "   停止服务: $DOCKER_COMPOSE down"
echo "   重启服务: $DOCKER_COMPOSE restart"
echo ""
