<template>
  <div class="layout">
    <header class="topbar">
      <button class="menu-btn" @click="mobileMenuOpen = !mobileMenuOpen" title="菜单">
        <el-icon class="action-icon"><Menu /></el-icon>
      </button>
      <strong class="brand">濮院毛衫 AI - 管理端</strong>
      <nav class="desktop-nav">
        <RouterLink to="/admin/dashboard">看板</RouterLink>
        <RouterLink to="/admin/tenants">租户</RouterLink>
        <RouterLink to="/admin/plugins">插件</RouterLink>
        <RouterLink to="/admin/pricing">定价</RouterLink>
        <RouterLink to="/admin/billing">账单</RouterLink>
        <RouterLink to="/admin/supplier-review">面料商</RouterLink>
        <RouterLink to="/admin/audit">审计</RouterLink>
        <RouterLink to="/admin/system-config">系统配置</RouterLink>
      </nav>
      <div class="actions">
        <ThemeToggle class="theme-toggle" />
        <button class="icon-btn" @click="handleLogout" title="退出登录">
          <el-icon class="action-icon"><SwitchButton /></el-icon>
        </button>
      </div>
    </header>

    <!-- Mobile Navigation Overlay -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="mobileMenuOpen" class="mobile-overlay" @click="mobileMenuOpen = false"></div>
      </Transition>
      <Transition name="slide">
        <nav v-if="mobileMenuOpen" class="mobile-nav">
          <div class="mobile-nav-header">
            <span>管理菜单</span>
            <button @click="mobileMenuOpen = false">
              <el-icon><Close /></el-icon>
            </button>
          </div>
          <div class="mobile-nav-links">
            <RouterLink to="/admin/dashboard" @click="mobileMenuOpen = false">
              <el-icon><DataLine /></el-icon>
              <span>看板</span>
            </RouterLink>
            <RouterLink to="/admin/tenants" @click="mobileMenuOpen = false">
              <el-icon><OfficeBuilding /></el-icon>
              <span>租户</span>
            </RouterLink>
            <RouterLink to="/admin/plugins" @click="mobileMenuOpen = false">
              <el-icon><Grid /></el-icon>
              <span>插件</span>
            </RouterLink>
            <RouterLink to="/admin/pricing" @click="mobileMenuOpen = false">
              <el-icon><Tickets /></el-icon>
              <span>定价</span>
            </RouterLink>
            <RouterLink to="/admin/billing" @click="mobileMenuOpen = false">
              <el-icon><Document /></el-icon>
              <span>账单</span>
            </RouterLink>
            <RouterLink to="/admin/supplier-review" @click="mobileMenuOpen = false">
              <el-icon><Goods /></el-icon>
              <span>面料商</span>
            </RouterLink>
            <RouterLink to="/admin/audit" @click="mobileMenuOpen = false">
              <el-icon><List /></el-icon>
              <span>审计</span>
            </RouterLink>
            <RouterLink to="/admin/system-config" @click="mobileMenuOpen = false">
              <el-icon><Setting /></el-icon>
              <span>系统配置</span>
            </RouterLink>
          </div>
          <div class="mobile-nav-footer">
            <button class="logout-btn" @click="handleLogout">
              <el-icon><SwitchButton /></el-icon>
              <span>退出登录</span>
            </button>
          </div>
        </nav>
      </Transition>
    </Teleport>

    <main class="content">
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Menu, Close, SwitchButton, DataLine, OfficeBuilding, Grid, Tickets, Document, Goods, List, Setting } from '@element-plus/icons-vue'
import ThemeToggle from "./components/ThemeToggle.vue";
import { useAdminAuthStore } from "./stores/adminAuth";

const router = useRouter()
const auth = useAdminAuthStore()
const mobileMenuOpen = ref(false)

function handleLogout() {
  mobileMenuOpen.value = false
  auth.clearAuth()
  router.push('/admin/login')
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: hsl(var(--background));
  color: hsl(var(--foreground));
}

/* Topbar */
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: hsl(var(--card));
  border-bottom: 1px solid hsl(var(--border));
  position: sticky;
  top: 0;
  z-index: 50;
  gap: 12px;
}

@media (min-width: 768px) {
  .topbar {
    padding: 16px 24px;
    gap: 24px;
  }
}

/* Brand */
.brand {
  font-size: 16px;
  font-weight: 600;
  color: hsl(var(--foreground));
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

@media (min-width: 768px) {
  .brand {
    font-size: 18px;
  }
}

/* Menu button - mobile only */
.menu-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  cursor: pointer;
  padding: 8px;
  border-radius: calc(var(--radius) - 4px);
  color: hsl(var(--muted-foreground));
  transition: all 0.2s ease;
}

.menu-btn:hover {
  background: hsl(var(--accent));
  color: hsl(var(--foreground));
}

@media (min-width: 1024px) {
  .menu-btn {
    display: none;
  }
}

/* Desktop Navigation */
.desktop-nav {
  display: none;
  align-items: center;
  gap: 4px;
}

@media (min-width: 1024px) {
  .desktop-nav {
    display: flex;
  }
}

.desktop-nav a {
  text-decoration: none;
  color: hsl(var(--muted-foreground));
  font-size: 14px;
  font-weight: 500;
  padding: 8px 12px;
  border-radius: calc(var(--radius) - 4px);
  transition: all 0.2s ease;
  white-space: nowrap;
}

.desktop-nav a:hover {
  color: hsl(var(--foreground));
  background: hsl(var(--accent));
}

.desktop-nav a.router-link-active {
  color: hsl(var(--primary));
  background: hsl(var(--primary) / 0.1);
}

/* Actions */
.actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

@media (min-width: 768px) {
  .actions {
    gap: 8px;
  }
}

.theme-toggle {
  margin: 0;
}

.icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  cursor: pointer;
  padding: 8px;
  border-radius: calc(var(--radius) - 4px);
  text-decoration: none;
  color: hsl(var(--muted-foreground));
  transition: all 0.2s ease;
}

.icon-btn:hover {
  background: hsl(var(--accent));
  color: hsl(var(--foreground));
}

.action-icon {
  font-size: 20px;
}

/* Mobile Overlay */
.mobile-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 100;
}

.mobile-overlay.fade-enter-active,
.mobile-overlay.fade-leave-active {
  transition: opacity 0.2s ease;
}

.mobile-overlay.fade-enter-from,
.mobile-overlay.fade-leave-to {
  opacity: 0;
}

/* Mobile Navigation Drawer */
.mobile-nav {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  width: 280px;
  max-width: 80vw;
  background: hsl(var(--card));
  z-index: 101;
  display: flex;
  flex-direction: column;
  box-shadow: 4px 0 24px rgba(0, 0, 0, 0.15);
}

.mobile-nav.slide-enter-active,
.mobile-nav.slide-leave-active {
  transition: transform 0.3s ease;
}

.mobile-nav.slide-enter-from,
.mobile-nav.slide-leave-to {
  transform: translateX(-100%);
}

.mobile-nav-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid hsl(var(--border));
  font-weight: 600;
}

.mobile-nav-header button {
  background: none;
  border: none;
  cursor: pointer;
  padding: 8px;
  color: hsl(var(--muted-foreground));
  font-size: 20px;
}

.mobile-nav-links {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.mobile-nav-links a {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  text-decoration: none;
  color: hsl(var(--muted-foreground));
  border-radius: calc(var(--radius) - 4px);
  transition: all 0.2s ease;
  font-size: 15px;
}

.mobile-nav-links a:hover {
  background: hsl(var(--accent));
  color: hsl(var(--foreground));
}

.mobile-nav-links a.router-link-active {
  background: hsl(var(--primary) / 0.1);
  color: hsl(var(--primary));
}

.mobile-nav-links .el-icon {
  font-size: 20px;
}

.mobile-nav-footer {
  padding: 16px;
  border-top: 1px solid hsl(var(--border));
}

.logout-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 12px;
  background: hsl(var(--destructive) / 0.1);
  color: hsl(var(--destructive));
  border: none;
  border-radius: calc(var(--radius) - 4px);
  cursor: pointer;
  font-size: 15px;
  transition: all 0.2s ease;
}

.logout-btn:hover {
  background: hsl(var(--destructive) / 0.2);
}

/* Content */
.content {
  flex: 1;
  padding: 16px;
}

@media (min-width: 768px) {
  .content {
    padding: 24px;
  }
}

@media (min-width: 1400px) {
  .content {
    max-width: 1400px;
    margin: 0 auto;
    width: 100%;
  }
}
</style>