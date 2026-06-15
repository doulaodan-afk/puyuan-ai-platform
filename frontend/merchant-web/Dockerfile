# ============================================================
# 濮院毛衫 AI 平台 - 商家端前端 Dockerfile (多阶段构建)
# 
# 阶段1: Node.js 构建
# 阶段2: Nginx 静态文件服务
# 
# 构建: docker build -t puyuan-merchant-web:latest .
# ============================================================

# ==================== 阶段 1: 构建 ====================
FROM node:18-alpine AS builder

WORKDIR /build

# 复制依赖文件
COPY package.json package-lock.json* ./
RUN npm ci --silent

# 复制源码
COPY . .

# 构建
RUN npm run build

# ==================== 阶段 2: 运行时 ====================
FROM nginx:alpine

LABEL maintainer="puyuan-maoshan <contact@puyuanmaoshan.com>"
LABEL description="濮院毛衫 AI 平台 - 商家端"

# 复制构建产物
COPY --from=builder /build/dist /usr/share/nginx/html/merchant

# 复制 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget -q --spider http://localhost/merchant/ || exit 1
