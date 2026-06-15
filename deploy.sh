#!/bin/bash
# ============================================================
# 濮院毛衫 AI 平台 - 自动化部署脚本 (Linux/Mac)
# 
# 功能: git pull → 后端打包 → 前端打包 → 数据库迁移 → 重启服务 → 健康检查
# 用法: bash deploy.sh [--skip-build] [--skip-frontend] [--skip-db] [--rollback]
# ============================================================

set -e

# ==================== 配置 ====================
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="${PROJECT_DIR}/backend/java-spring"
MERCHANT_WEB_DIR="${PROJECT_DIR}/frontend/merchant-web"
ADMIN_WEB_DIR="${PROJECT_DIR}/frontend/admin-web"
DEPLOY_DIR="${PROJECT_DIR}/deploy"
BACKUP_DIR="${DEPLOY_DIR}/backups/$(date +%Y%m%d_%H%M%S)"
JAR_FILE="${BACKEND_DIR}/target/platform-api-0.0.1-SNAPSHOT.jar"
HEALTH_CHECK_URL="http://localhost:8080/actuator/health"
MAX_HEALTH_RETRIES=30
HEALTH_RETRY_INTERVAL=2

# ==================== 颜色定义 ====================
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# ==================== 标志位 ====================
SKIP_BUILD=false
SKIP_FRONTEND=false
SKIP_DB=false
ROLLBACK=false

# ==================== 日志函数 ====================
log_info()  { echo -e "${GREEN}[INFO]${NC}  $(date '+%Y-%m-%d %H:%M:%S') - $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $(date '+%Y-%m-%d %H:%M:%S') - $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"; }
log_step()  { echo -e "\n${BLUE}========================================${NC}"; echo -e "${BLUE}[STEP]${NC} $1"; echo -e "${BLUE}========================================${NC}"; }

# ==================== 使用说明 ====================
usage() {
    echo "用法: bash deploy.sh [选项]"
    echo ""
    echo "选项:"
    echo "  --skip-build      跳过 Maven 后端编译"
    echo "  --skip-frontend   跳过前端构建"
    echo "  --skip-db         跳过数据库迁移"
    echo "  --rollback        回滚到上一个版本"
    echo "  -h, --help        显示帮助信息"
    echo ""
    echo "示例:"
    echo "  bash deploy.sh                    # 完整部署"
    echo "  bash deploy.sh --skip-frontend    # 仅部署后端"
    echo "  bash deploy.sh --skip-build       # 使用已有 jar 包部署"
    echo "  bash deploy.sh --rollback         # 回滚到上一版本"
    exit 0
}

# ==================== 参数解析 ====================
parse_args() {
    for arg in "$@"; do
        case $arg in
            --skip-build)    SKIP_BUILD=true ;;
            --skip-frontend) SKIP_FRONTEND=true ;;
            --skip-db)       SKIP_DB=true ;;
            --rollback)      ROLLBACK=true ;;
            -h|--help)       usage ;;
            *)               log_error "未知选项: $arg"; usage ;;
        esac
    done
}

# ==================== 环境检查 ====================
check_environment() {
    log_step "1. 环境检查"

    # 检查必要命令
    local missing=()
    for cmd in java mvn node npm git docker curl; do
        if ! command -v $cmd &> /dev/null; then
            missing+=($cmd)
        fi
    done

    if [ ${#missing[@]} -gt 0 ]; then
        log_error "缺少必要命令: ${missing[*]}"
        log_error "请先安装这些工具后重试"
        exit 1
    fi

    # 检查 Java 版本
    local java_version=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$java_version" -lt 17 ]; then
        log_error "需要 Java 17 或更高版本，当前版本: $(java -version 2>&1 | head -1)"
        exit 1
    fi

    # 检查 Node.js 版本
    local node_version=$(node -v | cut -d'v' -f2 | cut -d'.' -f1)
    if [ "$node_version" -lt 18 ]; then
        log_error "需要 Node.js 18 或更高版本，当前版本: $(node -v)"
        exit 1
    fi

    # 检查磁盘空间
    local available_space=$(df -BG "$PROJECT_DIR" | tail -1 | awk '{print $4}' | sed 's/G//')
    if [ "$available_space" -lt 5 ]; then
        log_warn "磁盘可用空间不足 5GB (当前: ${available_space}GB)，部署可能失败"
    fi

    log_info "环境检查通过 (Java $(java -version 2>&1 | head -1 | cut -d'"' -f2), Node $(node -v))"
}

# ==================== 代码更新 ====================
update_code() {
    log_step "2. 拉取最新代码"

    cd "$PROJECT_DIR"

    # 保存当前 commit hash（用于回滚）
    local current_commit=$(git rev-parse HEAD)
    echo "$current_commit" > "${DEPLOY_DIR}/.last_commit"

    # 检查是否有未提交的本地修改
    if ! git diff --quiet; then
        log_warn "检测到未提交的本地修改"
        read -p "是否继续？未提交的修改将被暂存 (y/N): " confirm
        if [[ "$confirm" != "y" && "$confirm" != "Y" ]]; then
            log_info "部署已取消"
            exit 0
        fi
        git stash push -m "auto-stash-before-deploy-$(date +%Y%m%d_%H%M%S)"
    fi

    # 拉取最新代码
    log_info "正在拉取最新代码..."
    git fetch origin
    git reset --hard origin/main
    log_info "代码已更新到 $(git log -1 --oneline)"
}

# ==================== 后端编译打包 ====================
build_backend() {
    log_step "3. 后端编译打包"

    cd "$BACKEND_DIR"

    log_info "正在清理旧的构建产物..."
    mvn clean -q

    log_info "正在编译..."
    if ! mvn compile -q; then
        log_error "后端编译失败！"
        exit 1
    fi

    log_info "正在运行测试..."
    if ! mvn test -q; then
        log_error "后端测试未通过！"
        log_warn "如需跳过测试，请使用: mvn clean package -DskipTests"
        exit 1
    fi

    log_info "正在打包..."
    if ! mvn package -DskipTests -q; then
        log_error "后端打包失败！"
        exit 1
    fi

    if [ ! -f "$JAR_FILE" ]; then
        log_error "Jar 文件未生成: $JAR_FILE"
        exit 1
    fi

    log_info "后端打包完成: $(ls -lh "$JAR_FILE" | awk '{print $5}')"
}

# ==================== 前端编译打包 ====================
build_frontend() {
    log_step "4. 前端编译打包"

    # 商家端
    log_info "正在构建商家端 (merchant-web)..."
    cd "$MERCHANT_WEB_DIR"
    if [ ! -d "node_modules" ]; then
        log_info "安装商家端依赖..."
        npm ci --silent
    fi
    if ! npm run build; then
        log_error "商家端构建失败！"
        exit 1
    fi
    log_info "商家端构建完成"

    # 管理端
    log_info "正在构建管理端 (admin-web)..."
    cd "$ADMIN_WEB_DIR"
    if [ ! -d "node_modules" ]; then
        log_info "安装管理端依赖..."
        npm ci --silent
    fi
    if ! npm run build; then
        log_error "管理端构建失败！"
        exit 1
    fi
    log_info "管理端构建完成"
}

# ==================== 备份当前版本 ====================
backup_current() {
    log_step "5. 备份当前版本"

    mkdir -p "$BACKUP_DIR"

    # 备份 jar
    if [ -f "${DEPLOY_DIR}/platform-api-0.0.1-SNAPSHOT.jar" ]; then
        cp "${DEPLOY_DIR}/platform-api-0.0.1-SNAPSHOT.jar" "${BACKUP_DIR}/"
        log_info "已备份 Jar 文件"
    fi

    # 备份前端
    if [ -d "${DEPLOY_DIR}/frontend/merchant-web/dist" ]; then
        mkdir -p "${BACKUP_DIR}/frontend/merchant-web"
        cp -r "${DEPLOY_DIR}/frontend/merchant-web/dist" "${BACKUP_DIR}/frontend/merchant-web/"
        log_info "已备份商家端前端文件"
    fi
    if [ -d "${DEPLOY_DIR}/frontend/admin-web/dist" ]; then
        mkdir -p "${BACKUP_DIR}/frontend/admin-web"
        cp -r "${DEPLOY_DIR}/frontend/admin-web/dist" "${BACKUP_DIR}/frontend/admin-web/"
        log_info "已备份管理端前端文件"
    fi

    # 备份数据库（如果有 Docker MySQL 运行）
    if docker-compose -f "${DEPLOY_DIR}/docker-compose.yml" ps mysql 2>/dev/null | grep -q "Up"; then
        log_info "正在备份数据库..."
        source "${DEPLOY_DIR}/.env" 2>/dev/null || true
        docker-compose -f "${DEPLOY_DIR}/docker-compose.yml" exec -T mysql \
            mysqldump -u${MYSQL_USER:-root} -p${MYSQL_PASSWORD:-} \
            ${MYSQL_DATABASE:-puyuan_ai_mvp} \
            --single-transaction --routines --triggers \
            > "${BACKUP_DIR}/database_backup.sql" 2>/dev/null && \
            log_info "数据库备份完成" || \
            log_warn "数据库备份失败（可能 MySQL 未运行）"
    fi

    # 清理旧备份（保留最近 5 次）
    local backup_count=$(ls -1d "${DEPLOY_DIR}/backups/"*/ 2>/dev/null | wc -l)
    if [ "$backup_count" -gt 5 ]; then
        log_info "清理旧备份..."
        ls -1dt "${DEPLOY_DIR}/backups/"*/ | tail -n +6 | xargs rm -rf
    fi

    log_info "备份完成: $BACKUP_DIR"
}

# ==================== 数据库迁移 ====================
run_database_migration() {
    log_step "6. 数据库迁移"

    # Flyway 会在应用启动时自动执行迁移
    # 这里仅做预检查和手动迁移支持

    # 检查是否有待执行的 SQL 脚本
    local sql_dir="${PROJECT_DIR}/sql"
    if [ -d "$sql_dir" ]; then
        local new_sql_count=$(find "$sql_dir" -name "*.sql" -newer "${DEPLOY_DIR}/.last_migration" 2>/dev/null | wc -l)
        if [ "$new_sql_count" -gt 0 ]; then
            log_info "检测到 ${new_sql_count} 个新的 SQL 脚本"
        fi
    fi

    # 如果 Docker MySQL 在运行，执行手动迁移
    if docker-compose -f "${DEPLOY_DIR}/docker-compose.yml" ps mysql 2>/dev/null | grep -q "Up"; then
        source "${DEPLOY_DIR}/.env" 2>/dev/null || true
        log_info "正在执行数据库迁移..."
        # 按顺序执行 sql 目录下的迁移脚本
        for sql_file in $(ls -1 "${PROJECT_DIR}/sql/"migration*.sql 2>/dev/null | sort); do
            local filename=$(basename "$sql_file")
            log_info "  执行: $filename"
            docker-compose -f "${DEPLOY_DIR}/docker-compose.yml" exec -T mysql \
                mysql -u${MYSQL_USER:-root} -p${MYSQL_PASSWORD:-} \
                ${MYSQL_DATABASE:-puyuan_ai_mvp} < "$sql_file" 2>/dev/null && \
                log_info "  ✓ $filename 执行成功" || \
                log_warn "  ⚠ $filename 执行可能有警告（可能已执行过）"
        done
    else
        log_info "MySQL 未运行，数据库迁移将在应用启动时由 Flyway 自动执行"
    fi

    # 记录迁移时间戳
    date +%s > "${DEPLOY_DIR}/.last_migration"
    log_info "数据库迁移完成"
}

# ==================== 部署到服务器 ====================
deploy_artifacts() {
    log_step "7. 部署产物"

    # 复制 jar 到 deploy 目录
    log_info "正在部署后端 Jar..."
    cp "$JAR_FILE" "${DEPLOY_DIR}/platform-api-0.0.1-SNAPSHOT.jar"
    log_info "Jar 文件已部署到 deploy/ 目录"

    # 复制前端 dist 到 deploy 目录
    log_info "正在部署前端文件..."
    mkdir -p "${DEPLOY_DIR}/frontend/merchant-web"
    mkdir -p "${DEPLOY_DIR}/frontend/admin-web"

    rm -rf "${DEPLOY_DIR}/frontend/merchant-web/dist"
    rm -rf "${DEPLOY_DIR}/frontend/admin-web/dist"

    cp -r "${MERCHANT_WEB_DIR}/dist" "${DEPLOY_DIR}/frontend/merchant-web/dist"
    cp -r "${ADMIN_WEB_DIR}/dist" "${DEPLOY_DIR}/frontend/admin-web/dist"

    log_info "前端文件已部署到 deploy/ 目录"
}

# ==================== 重启服务 ====================
restart_services() {
    log_step "8. 重启服务"

    cd "$DEPLOY_DIR"

    # 重启后端
    log_info "正在重启后端服务..."
    if docker-compose ps backend 2>/dev/null | grep -q "Up"; then
        docker-compose restart backend
    else
        docker-compose up -d backend
    fi

    # 重启 Nginx
    log_info "正在重启 Nginx..."
    if docker-compose ps nginx 2>/dev/null | grep -q "Up"; then
        docker-compose restart nginx
    else
        docker-compose up -d nginx
    fi

    log_info "服务重启完成"
}

# ==================== 健康检查 ====================
health_check() {
    log_step "9. 健康检查"

    local retry=0
    local backend_healthy=false
    local nginx_healthy=false

    # 检查后端
    log_info "正在检查后端服务..."
    while [ $retry -lt $MAX_HEALTH_RETRIES ]; do
        if curl -sf "$HEALTH_CHECK_URL" > /dev/null 2>&1; then
            local health_response=$(curl -s "$HEALTH_CHECK_URL")
            log_info "后端健康检查通过: $health_response"
            backend_healthy=true
            break
        fi
        retry=$((retry + 1))
        echo -n "."
        sleep $HEALTH_RETRY_INTERVAL
    done
    echo ""

    if [ "$backend_healthy" = false ]; then
        log_error "后端服务健康检查超时！"
        log_error "请检查日志: docker-compose -f ${DEPLOY_DIR}/docker-compose.yml logs backend"
        return 1
    fi

    # 检查 Nginx
    log_info "正在检查 Nginx 代理..."
    if curl -sf "http://localhost/health" > /dev/null 2>&1; then
        log_info "Nginx 代理健康检查通过"
        nginx_healthy=true
    else
        log_warn "Nginx 代理健康检查未通过，但后端直接访问正常"
    fi

    # 显示服务状态
    log_info "当前服务状态:"
    cd "$DEPLOY_DIR"
    docker-compose ps 2>/dev/null || true
}

# ==================== 回滚 ====================
do_rollback() {
    log_step "回滚到上一个版本"

    # 查找最新的备份
    local latest_backup=$(ls -1dt "${DEPLOY_DIR}/backups/"*/ 2>/dev/null | head -1)
    if [ -z "$latest_backup" ]; then
        log_error "没有找到备份文件，无法回滚"
        exit 1
    fi

    log_info "使用备份: $latest_backup"

    cd "$DEPLOY_DIR"

    # 停止服务
    log_info "正在停止服务..."
    docker-compose stop backend nginx

    # 恢复 Jar
    if [ -f "${latest_backup}/platform-api-0.0.1-SNAPSHOT.jar" ]; then
        cp "${latest_backup}/platform-api-0.0.1-SNAPSHOT.jar" "${DEPLOY_DIR}/"
        log_info "已恢复 Jar 文件"
    fi

    # 恢复前端
    if [ -d "${latest_backup}/frontend/merchant-web/dist" ]; then
        rm -rf "${DEPLOY_DIR}/frontend/merchant-web/dist"
        cp -r "${latest_backup}/frontend/merchant-web/dist" "${DEPLOY_DIR}/frontend/merchant-web/"
        log_info "已恢复商家端前端"
    fi
    if [ -d "${latest_backup}/frontend/admin-web/dist" ]; then
        rm -rf "${DEPLOY_DIR}/frontend/admin-web/dist"
        cp -r "${latest_backup}/frontend/admin-web/dist" "${DEPLOY_DIR}/frontend/admin-web/"
        log_info "已恢复管理端前端"
    fi

    # 恢复数据库
    if [ -f "${latest_backup}/database_backup.sql" ]; then
        log_info "正在恢复数据库..."
        source "${DEPLOY_DIR}/.env" 2>/dev/null || true
        docker-compose exec -T mysql \
            mysql -u${MYSQL_USER:-root} -p${MYSQL_PASSWORD:-} \
            ${MYSQL_DATABASE:-puyuan_ai_mvp} < "${latest_backup}/database_backup.sql"
        log_info "数据库已恢复"
    fi

    # 启动服务
    log_info "正在启动服务..."
    docker-compose start backend nginx

    # 健康检查
    health_check

    log_info "回滚完成！"
}

# ==================== 部署总结 ====================
deploy_summary() {
    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}  ✓  部署完成！${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo "  项目目录:     $PROJECT_DIR"
    echo "  部署目录:     $DEPLOY_DIR"
    echo "  备份目录:     $BACKUP_DIR"
    echo "  Git 提交:     $(git -C "$PROJECT_DIR" log -1 --oneline 2>/dev/null || echo 'N/A')"
    echo ""
    echo "  后端健康检查:  $HEALTH_CHECK_URL"
    echo "  Nginx 代理:    http://localhost"
    echo "  商家端:        http://localhost/merchant/"
    echo "  管理端:        http://localhost/admin/"
    echo ""
    echo "  常用命令:"
    echo "    查看日志:    docker-compose -f ${DEPLOY_DIR}/docker-compose.yml logs -f"
    echo "    服务状态:    docker-compose -f ${DEPLOY_DIR}/docker-compose.yml ps"
    echo "    停止服务:    docker-compose -f ${DEPLOY_DIR}/docker-compose.yml down"
    echo "    回滚版本:    bash deploy.sh --rollback"
    echo ""
}

# ==================== 主流程 ====================
main() {
    parse_args "$@"

    echo ""
    echo -e "${BLUE}╔════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║   濮院毛衫 AI 平台 - 自动化部署       ║${NC}"
    echo -e "${BLUE}║   时间: $(date '+%Y-%m-%d %H:%M:%S')          ║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════╝${NC}"
    echo ""

    # 回滚模式
    if [ "$ROLLBACK" = true ]; then
        do_rollback
        exit 0
    fi

    # 正常部署流程
    check_environment
    update_code

    if [ "$SKIP_BUILD" = false ]; then
        build_backend
    else
        log_warn "跳过后端编译（--skip-build）"
        if [ ! -f "$JAR_FILE" ]; then
            log_error "Jar 文件不存在且跳过了编译: $JAR_FILE"
            exit 1
        fi
    fi

    if [ "$SKIP_FRONTEND" = false ]; then
        build_frontend
    else
        log_warn "跳过前端构建（--skip-frontend）"
    fi

    backup_current

    if [ "$SKIP_DB" = false ]; then
        run_database_migration
    else
        log_warn "跳过数据库迁移（--skip-db）"
    fi

    deploy_artifacts
    restart_services
    health_check
    deploy_summary
}

# 执行主函数
main "$@"
