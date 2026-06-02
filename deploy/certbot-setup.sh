#!/bin/bash

# SSL 证书自动配置脚本（使用 certbot + Let's Encrypt）

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查 certbot 是否已安装
check_certbot() {
    if ! command -v certbot &> /dev/null; then
        log_info "安装 certbot..."
        apt-get update && apt-get install -y certbot
    fi
}

# 获取域名
DOMAIN=${CERTBOT_DOMAIN:-localhost}
EMAIL=${CERTBOT_EMAIL:-admin@${DOMAIN}}

log_info "配置 SSL 证书"
log_info "域名: $DOMAIN"
log_info "邮箱: $EMAIL"

# 创建必要的目录
mkdir -p ./nginx/ssl ./nginx/logs ./nginx/www/certbot

# 使用 certbot 获取证书
log_info "使用 certbot 获取 SSL 证书..."
certbot certonly \
    --standalone \
    --email $EMAIL \
    --agree-tos \
    --no-eff-email \
    -d $DOMAIN \
    --config-dir ./nginx/ssl/certbot \
    --work-dir ./nginx/ssl/certbot/work \
    --logs-dir ./nginx/ssl/certbot/logs

# 检查证书是否成功生成
if [ -f "./nginx/ssl/certbot/live/$DOMAIN/fullchain.pem" ] && \
   [ -f "./nginx/ssl/certbot/live/$DOMAIN/privkey.pem" ]; then

    log_info "SSL 证书获取成功"

    # 复制证书到 nginx/ssl 目录
    cp ./nginx/ssl/certbot/live/$DOMAIN/fullchain.pem ./nginx/ssl/fullchain.pem
    cp ./nginx/ssl/certbot/live/$DOMAIN/privkey.pem ./nginx/ssl/privkey.pem

    log_info "证书已复制到 nginx/ssl 目录"

    # 设置自动续期任务
    log_info "设置证书自动续期任务..."

    # 创建 certbot 自动续期脚本
    cat > ./certbot-renew.sh << 'EOF'
#!/bin/bash
certbot renew --config-dir /app/nginx/ssl/certbot --work-dir /app/nginx/ssl/certbot/work --logs-dir /app/nginx/ssl/certbot/logs
docker exec -t puyuan-nginx nginx -s reload
EOF

    chmod +x ./certbot-renew.sh

    # 添加到 crontab（每天凌晨 2 点检查）
    (crontab -l 2>/dev/null | grep -v "certbot-renew.sh"; echo "0 2 * * * /app/certbot-renew.sh >> /var/log/certbot-renew.log 2>&1") | crontab -

    log_info "自动续期任务已设置（每天凌晨 2 点）"

    log_info "================================"
    log_info "SSL 证书配置完成"
    log_info "证书路径: ./nginx/ssl/fullchain.pem"
    log_info "私钥路径: ./nginx/ssl/privkey.pem"
    log_info "================================"

else
    log_error "SSL 证书获取失败"
    log_error "请检查："
    log_error "1. 域名 $DOMAIN 的 DNS 解析是否正确"
    log_error "2. 服务器的 80 和 443 端口是否开放"
    log_error "3. 防火墙是否阻止入站连接"
    exit 1
fi
