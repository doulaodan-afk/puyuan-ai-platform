#!/bin/bash
# ============================================
# 濮院毛衫 AI 平台 - 服务器端版本迭代脚本
# ============================================
# 用法：
#   ./server-update.sh           # 完整更新（拉取代码 + 重新构建 + 重启服务）
#   ./server-update.sh --pull    # 仅拉取代码，不重启
#   ./server-update.sh --restart # 仅重启服务（不拉取代码）
#   ./server-update.sh --status  # 查看当前版本和服务状态
#
# 前提条件：
#   1. 服务器已安装 docker 和 docker-compose
#   2. 项目代码已 clone 到 /app/puyuan-ai-platform
#   3. .env 文件已配置好（参考 .env.example）
#   4. 商户证书已放到 /app/config/apiclient_key.pem
# ============================================

set -e

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

PROJECT_DIR="/app/puyuan-ai-platform"
DEPLOY_DIR="$PROJECT_DIR/deploy"

log_info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_step()  { echo -e "${CYAN}[STEP]${NC} $1"; }

# 查看当前版本和状态
show_status() {
    echo ""
    log_info "========== 当前版本 =========="
    cd "$PROJECT_DIR"
    echo "Git 分支:  $(git branch --show-current)"
    echo "Git 提交:  $(git log --oneline -1)"
    echo "提交时间:  $(git log -1 --format=%ci)"
    echo ""

    log_info "========== Docker 服务状态 =========="
    cd "$DEPLOY_DIR"
    docker-compose ps 2>/dev/null || echo "服务未启动"
    echo ""

    log_info "========== 后端健康检查 =========="
    HEALTH=$(curl -sf http://localhost:8080/actuator/health 2>/dev/null || echo "UNREACHABLE")
    echo "后端: $HEALTH"
    echo ""
}

# 拉取最新代码
pull_code() {
    log_step "拉取最新代码..."
    cd "$PROJECT_DIR"

    # 记录更新前的版本
    OLD_COMMIT=$(git log --oneline -1)

    git fetch origin
    git reset --hard origin/main

    NEW_COMMIT=$(git log --oneline -1)

    if [ "$OLD_COMMIT" = "$NEW_COMMIT" ]; then
        log_info "代码无变化: $OLD_COMMIT"
        return 1
    else
        log_info "代码已更新:"
        echo "  旧: $OLD_COMMIT"
        echo "  新: $NEW_COMMIT"
        echo ""
        git log --oneline ${OLD_COMMIT%% *}..HEAD 2>/dev/null | head -10
        return 0
    fi
}

# 构建并重启服务
build_and_restart() {
    log_step "构建 Docker 镜像并重启服务..."
    cd "$DEPLOY_DIR"

    # 加载环境变量
    if [ -f .env ]; then
        log_info "加载 .env 文件"
        set -a
        source .env
        set +a
    else
        log_error ".env 文件不存在！请复制 .env.example 并配置"
        exit 1
    fi

    # 停止旧服务
    log_step "停止旧服务..."
    docker-compose down 2>/dev/null || true

    # 构建新镜像
    log_step "构建新镜像（可能需要几分钟）..."
    docker-compose build --no-cache

    # 启动新服务
    log_step "启动新服务..."
    docker-compose up -d

    # 等待服务就绪
    log_step "等待服务启动..."
    sleep 10

    # 健康检查
    local max_attempts=30
    local attempt=0
    while [ $attempt -lt $max_attempts ]; do
        if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
            log_info "后端服务启动成功！"
            break
        fi
        attempt=$((attempt + 1))
        echo "  等待中... ($attempt/$max_attempts)"
        sleep 3
    done

    if [ $attempt -eq $max_attempts ]; then
        log_error "后端服务启动超时，请检查日志: docker-compose logs backend"
        return 1
    fi

    # 清理旧镜像
    docker image prune -f > /dev/null 2>&1
}

# 仅重启服务
restart_only() {
    log_step "重启服务..."
    cd "$DEPLOY_DIR"

    if [ -f .env ]; then
        set -a
        source .env
        set +a
    fi

    docker-compose restart
    log_info "服务已重启"
}

# 显示部署结果
show_result() {
    echo ""
    log_info "========== 部署完成 =========="
    echo "  后端:    http://localhost:8080"
    echo "  Nginx:   http://localhost"
    echo "  HTTPS:   https://ai.puyuanmaoshan.com"
    echo ""
    echo "  查看日志:  cd $DEPLOY_DIR && docker-compose logs -f"
    echo "  查看状态:  ./server-update.sh --status"
    echo "  回滚版本:  git checkout <commit-hash> && ./server-update.sh --restart"
    echo ""
}

# ============================================
# 主逻辑
# ============================================
case "${1:-}" in
    --status)
        show_status
        ;;
    --pull)
        pull_code
        ;;
    --restart)
        restart_only
        ;;
    --rollback)
        if [ -z "${2:-}" ]; then
            log_error "请指定回滚的 commit hash: ./server-update.sh --rollback <commit-hash>"
            echo ""
            log_info "最近的提交:"
            cd "$PROJECT_DIR" && git log --oneline -10
            exit 1
        fi
        log_step "回滚到: $2"
        cd "$PROJECT_DIR"
        git reset --hard "$2"
        build_and_restart
        show_result
        ;;
    *)
        # 默认：完整更新流程
        echo ""
        log_info "濮院毛衫 AI 平台 - 版本迭代"
        log_info "================================"
        echo ""

        CODE_CHANGED=$(pull_code; echo $?)
        if [ "$CODE_CHANGED" = "0" ]; then
            build_and_restart
            show_result
        else
            log_info "代码无变化，无需重新构建"
            show_status
        fi
        ;;
esac
