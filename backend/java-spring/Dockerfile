# ============================================================
# 濮院毛衫 AI 平台 - 后端 Dockerfile (多阶段构建)
# 
# 阶段1: Maven 编译
# 阶段2: 运行时镜像
# 
# 构建: docker build -t puyuan-backend:latest .
# 运行: docker run -p 8080:8080 --env-file .env puyuan-backend:latest
# ============================================================

# ==================== 阶段 1: 构建 ====================
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# 先复制 pom.xml 以利用 Docker 缓存层
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

# 复制源码并构建
COPY src ./src
RUN mvn clean package -DskipTests -q

# ==================== 阶段 2: 运行时 ====================
FROM eclipse-temurin:17-jdk-alpine

LABEL maintainer="puyuan-maoshan <contact@puyuanmaoshan.com>"
LABEL description="濮院毛衫 AI 平台 - 后端服务"

# 安装必要工具
RUN apk add --no-cache curl tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 创建非 root 用户
RUN addgroup -S appuser && adduser -S -G appuser appuser

WORKDIR /app

# 从构建阶段复制 jar
COPY --from=builder /build/target/platform-api-0.0.1-SNAPSHOT.jar ./app.jar

# 创建配置目录
RUN mkdir -p /app/config && chown -R appuser:appuser /app

USER appuser

EXPOSE 8080

# JVM 参数（可通过环境变量 JAVA_OPTS 覆盖）
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp"

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
