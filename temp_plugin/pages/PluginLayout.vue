<template>
  <div class="design-assistant-plugin">
    <!-- 插件顶部导航栏 -->
    <header class="plugin-header">
      <div class="plugin-brand">
        <span class="icon">✨</span>
        <span class="title">AI 设计助手</span>
      </div>

      <div class="plugin-nav">
        <!-- 身份标识 -->
        <div v-if="store.identity" class="identity-badge" @click="handleSwitchIdentity" title="点击切换身份">
          <span class="identity-icon">🏢</span>
          <span class="identity-text">{{ store.identity.tenantName }}</span>
          <span class="identity-separator">·</span>
          <span class="identity-role">{{ store.identity.roleLabel }}</span>
          <span class="identity-switch-hint">🔄</span>
        </div>

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
            <span v-if="menu.badge && typeof menu.badge === 'number' && menu.badge > 0" class="menu-badge">{{ menu.badge }}</span>
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
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useDesignAssistantStore } from '../stores'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const store = useDesignAssistantStore()

// 定义设计助手插件内的所有菜单项
interface MenuItem {
  path: string
  name: string
  icon: string
  roles: string[]
  badge?: number | (() => number)
}

const pluginMenus: MenuItem[] = [
  // boss 管理看板 - 查看工作室所有工作情况
  {
    path: '/plugins/ai-design-assistant/board',
    name: '管理看板',
    icon: '👑',
    roles: ['boss'],
  },
  // 创建设计需求 - 设计师和 boss 可创建
  {
    path: '/plugins/ai-design-assistant/create',
    name: '创建设计需求',
    icon: '✨',
    roles: ['designer'],
  },
  // 我的设计需求 - 各角色看自己创建的
  {
    path: '/plugins/ai-design-assistant/list',
    name: '我的设计需求',
    icon: '📋',
    roles: ['designer', 'design_assistant', 'operator', 'viewer'],
  },
  // 设计助理待办
  {
    path: '/plugins/ai-design-assistant/pending',
    name: '设计助理待办',
    icon: '🔔',
    roles: ['design_assistant'],
  },
  // 我的任务
  {
    path: '/plugins/ai-design-assistant/tasks',
    name: '我的任务',
    icon: '✅',
    roles: ['designer', 'pattern_maker', 'operator', 'viewer'],
  },
  // 消息中心
  {
    path: '/plugins/ai-design-assistant/messages',
    name: '消息中心',
    icon: '💬',
    roles: ['boss', 'designer', 'design_assistant', 'pattern_maker', 'operator', 'viewer'],
  },
  // 面料库管理
  {
    path: '/plugins/ai-design-assistant/fabrics',
    name: '面料库管理',
    icon: '🧵',
    roles: ['operator'],
  },
  // boss 专属管理菜单
  {
    path: '/plugins/ai-design-assistant/settings',
    name: '成员管理',
    icon: '👥',
    roles: ['boss'],
  },
  {
    path: '/plugins/ai-design-assistant/partners',
    name: '合作方管理',
    icon: '🤝',
    roles: ['boss'],
  },
]

// 根据当前身份角色过滤菜单（优先使用插件身份角色，回退到系统角色）
const visibleMenus = computed(() => {
  const currentRole = store.identity?.role || auth.currentRole
  if (!currentRole) {
    return pluginMenus
  }
  return pluginMenus.filter((menu) => menu.roles.includes(currentRole))
})

// 判断当前路由是否激活
function isActiveRoute(menuPath: string): boolean {
  if (menuPath === '/plugins/ai-design-assistant' || menuPath === '/plugins/ai-design-assistant/list') {
    return route.path === '/plugins/ai-design-assistant' || route.path === '/plugins/ai-design-assistant/list'
  }
  return route.path.startsWith(menuPath)
}

// 切换身份
function handleSwitchIdentity() {
  // 清除当前身份并跳转到身份选择页
  store.clearIdentity()
  store.resetAll()
  router.push('/plugins/ai-design-assistant/identity')
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
  flex-wrap: wrap;
  gap: 12px;
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

/* 身份标识 */
.identity-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: linear-gradient(135deg, #eef2ff, #f5f3ff);
  border: 1px solid #c4b5fd;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.identity-badge:hover {
  background: linear-gradient(135deg, #e0e7ff, #ede9fe);
  border-color: #a78bfa;
  box-shadow: 0 2px 6px rgba(139, 92, 246, 0.15);
}

.identity-icon {
  font-size: 14px;
}

.identity-text {
  font-size: 13px;
  font-weight: 600;
  color: #4338ca;
}

.identity-separator {
  color: #a5b4fc;
  font-weight: 300;
}

.identity-role {
  font-size: 13px;
  color: #6d28d9;
}

.identity-switch-hint {
  font-size: 11px;
  opacity: 0;
  transition: opacity 0.2s;
  margin-left: 2px;
}

.identity-badge:hover .identity-switch-hint {
  opacity: 0.7;
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
