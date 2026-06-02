#!/bin/bash

echo "================================"
echo "濮院毛衫 AI 平台 - 部署检查"
echo "================================"
echo ""

# 检查 Docker 容器状态
echo "📦 1️⃣  检查 Docker 容器状态"
echo "---"
docker-compose ps
echo ""

# 检查 MySQL
echo "🗄️  2️⃣  检查 MySQL 数据库"
echo "---"
if docker-compose exec -T mysql mysqladmin ping -h localhost -u root -proot123 2>/dev/null; then
    echo "✅ MySQL 连接成功"
    echo "数据库列表:"
    docker-compose exec -T mysql mysql -u root -proot123 -e "SHOW DATABASES;" 2>/dev/null
else
    echo "❌ MySQL 连接失败"
fi
echo ""

# 检查 Redis
echo "💾 3️⃣  检查 Redis 缓存"
echo "---"
if docker-compose exec -T redis redis-cli -a redis123 ping 2>/dev/null; then
    echo "✅ Redis 连接成功"
    docker-compose exec -T redis redis-cli -a redis123 INFO stats 2>/dev/null | grep -E "connected_clients|total_commands_processed"
else
    echo "❌ Redis 连接失败"
fi
echo ""

# 检查后端应用
echo "🚀 4️⃣  检查后端应用"
echo "---"
if curl -s http://localhost:8080/actuator/health | grep -q "UP"; then
    echo "✅ 后端应用运行正常"
    curl -s http://localhost:8080/actuator/health | grep -o '"status":"[^"]*"'
else
    echo "❌ 后端应用未响应"
    echo "查看后端日志:"
    docker-compose logs --tail=20 backend
fi
echo ""

# 检查前端文件
echo "🌐 5️⃣  检查前端文件"
echo "---"
if docker-compose exec -T nginx test -d /usr/share/nginx/html/merchant; then
    echo "✅ 商家端前端文件已部署"
    docker-compose exec -T nginx ls -lh /usr/share/nginx/html/merchant/ | head -5
else
    echo "❌ 商家端前端文件未找到"
fi

if docker-compose exec -T nginx test -d /usr/share/nginx/html/admin; then
    echo "✅ 管理端前端文件已部署"
else
    echo "❌ 管理端前端文件未找到"
fi
echo ""

# 检查网络连接
echo "🔌 6️⃣  检查网络连接"
echo "---"
echo "后端 API (localhost:8080):"
curl -s -o /dev/null -w "HTTP 状态码: %{http_code}\n" http://localhost:8080/actuator/health

echo ""
echo "Nginx (localhost:80):"
curl -s -o /dev/null -w "HTTP 状态码: %{http_code}\n" http://localhost/health

echo ""
echo "================================"
echo "检查完成！"
echo "================================"
echo ""
echo "✨ 应用访问地址："
echo "   商家端: http://your-server-ip/merchant"
echo "   管理端: http://your-server-ip/admin"
echo "   API 文档: http://your-server-ip/swagger-ui.html"
echo ""
echo "📋 常用命令："
echo "   查看容器日志:  docker-compose logs -f backend"
echo "   重启服务:     docker-compose restart"
echo "   停止服务:     docker-compose down"
echo ""
