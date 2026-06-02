<template>
  <div class="design-assistant-plugin">
    <!-- 插件顶部导航栏 -->
    <header class="plugin-header">
      <div class="plugin-brand">
        <span class="icon">✨</span>
        <span class="title">设计助手</span>
      </div>

      <div class="plugin-nav">
        <!-- 插件菜单 -->
        <nav class="plugin-menu">
          <RouterLink
            v-for="menu in visibleMenus"
            :key="menu.path"
            :to="menu.path"
            :class="{ active: isActiveRoute(menu.path) }"
          >
            <span class="menu-icon">{{ menu.icon }}</span>
            <span class="menu-name">{{ menu.name }}</span>
            <span v-if="menu.badge && menu.badge > 0" class="menu-badge">{{ menu.badge }}</span>
          </RouterLink>
        </nav>

        <!-- 返回主站导航 -->
        <RouterLink to="/dashboard" class="back-btn" title="返回工作台">
          <el-icon><ArrowLeft /></el-icon>
        </RouterLink>
      </div>
    </header>

    <!-- 插件内容区域 -->
    <main class="plugin-content">
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { useAuthStore } from '../../stores/auth'
import { useMenuFilter } from './useMenuFilter'

const route = useRoute()
const auth = useAuthStore()

// 获取菜单过滤结果
const { visibleMenus } = useMenuFilter()

// 判断当前路由是否激活
function isActiveRoute(menuPath: string): boolean {
  if (menuPath === '/design-assistant' || menuPath === '/design-assistant/list') {
    return route.path === '/design-assistant' || route.path === '/design-assistant/list'
  }
  return route.path.startsWith(menuPath)
}
</script>

<style scoped>
.design-assistant-plugin {
  min-height: 100vh;
  background: hsl(var(--background));
}

.plugin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: hsl(var(--card));
  border-bottom: 1px solid hsl(var(--border));
}

.plugin-brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.plugin-brand .icon {
  font-size: 24px;
}

.plugin-brand .title {
  font-size: 18px;
  font-weight: 600;
  color: hsl(var(--foreground));
}

.plugin-nav {
  display: flex;
  align-items: center;
  gap: 16px;
}

.plugin-menu {
  display: flex;
  align-items: center;
  gap: 4px;
}

.plugin-menu a {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  text-decoration: none;
  color: hsl(var(--muted-foreground));
  font-size: 14px;
  border-radius: calc(var(--radius) - 4px);
  transition: all 0.2s;
}

.plugin-menu a:hover {
  background: hsl(var(--accent));
  color: hsl(var(--foreground));
}

.plugin-menu a.active {
  background: hsl(var(--primary) / 0.1);
  color: hsl(var(--primary));
  font-weight: 500;
}

.menu-icon {
  font-size: 16px;
}

.menu-name {
  white-space: nowrap;
}

.menu-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  background: hsl(var(--destructive));
  color: hsl(var(--destructive-foreground));
  font-size: 11px;
  font-weight: bold;
  border-radius: 10px;
}

.back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px;
  text-decoration: none;
  color: hsl(var(--muted-foreground));
  border-radius: calc(var(--radius) - 4px);
  transition: all 0.2s;
}

.back-btn:hover {
  background: hsl(var(--accent));
  color: hsl(var(--foreground));
}

.back-btn .el-icon {
  font-size: 20px;
}

.plugin-content {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

@media (max-width: 1024px) {
  .plugin-menu {
    display: none;
  }
}
</style>