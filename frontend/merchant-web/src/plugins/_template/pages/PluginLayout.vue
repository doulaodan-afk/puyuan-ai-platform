<template>
  <div class="{{PLUGIN_ID}}-plugin">
    <header class="plugin-header">
      <div class="plugin-brand">
        <span class="icon">🧩</span>
        <span class="title">{{PLUGIN_NAME}}</span>
      </div>
      <div class="plugin-nav">
        <nav class="plugin-menu">
          <RouterLink
            v-for="menu in menus"
            :key="menu.path"
            :to="menu.path"
            :class="{ active: isActive(menu.path) }"
          >
            <span class="menu-name">{{ menu.name }}</span>
          </RouterLink>
        </nav>
        <RouterLink to="/dashboard" class="back-btn" title="返回工作台">
          ← 返回
        </RouterLink>
      </div>
    </header>
    <main class="plugin-content">
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()

const menus = [
  { path: '/plugins/{{PLUGIN_ID}}/home', name: '首页' },
]

function isActive(path: string): boolean {
  return route.path === path || route.path.startsWith(path + '/')
}
</script>

<style scoped>
.{{PLUGIN_ID}}-plugin {
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

.back-btn {
  padding: 8px 14px;
  text-decoration: none;
  color: hsl(var(--muted-foreground));
  border-radius: calc(var(--radius) - 4px);
  transition: all 0.2s;
}

.back-btn:hover {
  background: hsl(var(--accent));
  color: hsl(var(--foreground));
}

.plugin-content {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}
</style>