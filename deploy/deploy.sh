#!/bin/bash

# 濮院毛衫 AI 平台 - 一键部署脚本

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查必要的命令
check_commands() {
    local commands=("docker" "docker-compose" "git" "curl")
    for cmd in "${commands[@]}"; do
        if ! command -v $cmd &> /dev/null; then
            log_error "$cmd 命令未找到，请先安装"
            exit 1
        fi
    done
    log_info "所有必要的命令都已安装"
}

# 加载环境变量
load_env() {
    if [ -f .env ]; then
        log_info "加载 .env 文件"
        export $(cat .env | grep -v '^#' | xargs)
    else
        log_warn ".env 文件不存在，使用默认值"
    fi

    # 设置默认环境变量
    export MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD:-change_password}
    export MYSQL_DATABASE=${MYSQL_DATABASE:-puyuan_ai_prod}
    export MYSQL_USER=${MYSQL_USER:-puyuan_user}
    export MYSQL_PASSWORD=${MYSQL_PASSWORD:-change_password}
    export REDIS_PASSWORD=${REDIS_PASSWORD:-change_password}
    export CONTENT_SECURITY_ENABLED=${CONTENT_SECURITY_ENABLED:-true}
    export WX_MINIAPP_APPID=${WX_MINIAPP_APPID:-}
    export WX_MINIAPP_SECRET=${WX_MINIAPP_SECRET:-}
    export WX_PAYMENT_MCHID=${WX_PAYMENT_MCHID:-}
    export WX_PAYMENT_API_V3_KEY=${WX_PAYMENT_API_V3_KEY:-}
    export WX_PAYMENT_NOTIFY_URL=${WX_PAYMENT_NOTIFY_URL:-}
    export WX_PAYMENT_PRIVATE_KEY_PATH=${WX_PAYMENT_PRIVATE_KEY_PATH:-/app/config/apiclient_key.pem}
    export WX_PAYMENT_MERCHANT_SERIAL_NUMBER=${WX_PAYMENT_MERCHANT_SERIAL_NUMBER:-}
    export WX_PAYMENT_MOCK_ENABLED=${WX_PAYMENT_MOCK_ENABLED:-true}
    export WX_SUBSCRIBE_MOCK_ENABLED=${WX_SUBSCRIBE_MOCK_ENABLED:-true}
    export WX_SUBSCRIBE_BALANCE_LOW_TEMPLATE_ID=${WX_SUBSCRIBE_BALANCE_LOW_TEMPLATE_ID:-}
    export WX_SUBSCRIBE_RECHARGE_SUCCESS_TEMPLATE_ID=${WX_SUBSCRIBE_RECHARGE_SUCCESS_TEMPLATE_ID:-}
    export AI_MOCK_ENABLED=${AI_MOCK_ENABLED:-false}
    export OPENAI_API_KEY=${OPENAI_API_KEY:-}
}

# 拉取最新代码
pull_code() {
    log_info "拉取最新代码..."
    git fetch origin
    git reset --hard origin/main
    log_info "代码拉取完成"
}

# 构建 Docker 镜像
build_images() {
    log_info "构建 Docker 镜像..."
    docker-compose build
    log_info "镜像构建完成"
}

# 停止现有服务
stop_services() {
    log_info "停止现有服务..."
    docker-compose down
    log_info "服务已停止"
}

# 启动服务
start_services() {
    log_info "启动服务..."
    docker-compose up -d

    # 等待服务启动
    log_info "等待服务启动..."
    sleep 10

    # 检查服务状态
    if docker-compose ps | grep -q "Up"; then
        log_info "所有服务启动成功"
    else
        log_error "部分服务启动失败，请检查日志"
        docker-compose logs
        exit 1
    fi
}

# 检查服务健康状态
check_health() {
    log_info "检查服务健康状态..."

    # 检查后端服务
    local max_attempts=30
    local attempt=0

    while [ $attempt -lt $max_attempts ]; do
        if curl -f http://localhost:8080/actuator/health &> /dev/null; then
            log_info "后端服务健康检查通过"
            break
        fi
        attempt=$((attempt + 1))
        sleep 2
    done

    if [ $attempt -eq $max_attempts ]; then
        log_error "后端服务健康检查超时"
        return 1
    fi

    log_info "所有健康检查通过"
}

# 初始化数据库
init_db() {
    log_info "初始化数据库..."

    # 检查数据库是否已存在
    if docker-compose exec -T mysql mysql -u${MYSQL_USER} -p${MYSQL_PASSWORD} -e "USE ${MYSQL_DATABASE}" &> /dev/null; then
        log_info "数据库已存在，跳过初始化"
        return
    fi

    # 执行初始化 SQL
    log_info "执行数据库初始化脚本..."
    docker-compose exec -T mysql mysql -u${MYSQL_USER} -p${MYSQL_PASSWORD} < init-db/init.sql
    log_info "数据库初始化完成"
}

# 清理旧镜像
cleanup_old_images() {
    log_info "清理旧的 Docker 镜像..."
    docker image prune -f
    log_info "镜像清理完成"
}

# 显示部署信息
show_deployment_info() {
    echo ""
    log_info "========== 部署完成 =========="
    echo ""
    echo "后端服务: http://localhost:8080"
    echo "Nginx 代理: http://localhost"
    echo "Nginx HTTPS: https://localhost"
    echo ""
    echo "查看日志: docker-compose logs -f"
    echo "查看状态: docker-compose ps"
    echo "停止服务: docker-compose down"
    echo ""
    log_info "================================"
}

# 主函数
main() {
    echo ""
    log_info "濮院毛衫 AI 平台 - 一键部署"
    log_info "================================"
    echo ""

    # 检查命令
    check_commands

    # 加载环境变量
    load_env

    # 拉取代码
    pull_code

    # 停止现有服务
    stop_services

    # 清理旧镜像
    cleanup_old_images

    # 构建镜像
    build_images

    # 启动服务
    start_services

    # 检查健康状态
    check_health

    # 显示部署信息
    show_deployment_info
}

# 执行主函数
main "$@"
