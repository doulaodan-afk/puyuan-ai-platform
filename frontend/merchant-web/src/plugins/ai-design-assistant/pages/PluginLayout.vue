<template>
  <div class="design-assistant-plugin">
    <!-- 插件顶部导航栏 -->
    <header class="plugin-header">
      <div class="plugin-brand">
        <span class="icon">✨</span>
        <span class="title">AI 设计助手{{ currentStudioTitle ? ' · ' + currentStudioTitle : '' }}</span>
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
            <span v-if="menu.badge && typeof menu.badge === 'number' && menu.badge > 0" class="menu-badge">{{ menu.badge }}</span>
          </RouterLink>
        </nav>

        <!-- 工作室切换按钮（只要有工作室就显示） -->
        <el-dropdown
          v-if="auth.tenants.length > 0"
          trigger="click"
          @command="handleSwitchTenant"
          @visible-change="onDropdownVisibleChange"
        >
          <button class="studio-switch-btn" :title="currentStudioLabel">
            <span class="studio-switch-icon">🏠</span>
            <span class="studio-switch-name">{{ currentStudioLabel }}</span>
            <el-icon class="studio-switch-arrow"><ArrowDown /></el-icon>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item
                v-for="t in auth.tenants"
                :key="t.tenantId"
                :command="t.tenantId"
                :class="{ 'is-active': String(t.tenantId) === pluginTenantId }"
              >
                <span class="dropdown-studio-name">{{ t.tenantName }}</span>
                <span class="dropdown-studio-role">{{ getRoleLabel(t.role) }}</span>
                <el-icon v-if="String(t.tenantId) === pluginTenantId" class="dropdown-check"><Check /></el-icon>
                <button
                  v-if="isBossRole(t.role) && auth.tenants.length > 1"
                  class="dropdown-delete-btn"
                  @click.stop="handleDeleteTenant(t)"
                  title="删除工作室"
                >✕</button>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

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
import { ArrowLeft, ArrowDown, Check } from '@element-plus/icons-vue'
import { computed, ref, onMounted, onUnmounted, watch } from 'vue'
import { useAuthStore, type UserTenantInfo } from '@/stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

// 角色中文映射
const ROLE_LABEL_MAP: Record<string, string> = {
  boss: '管理员',
  designer: '设计师',
  design_assistant: '设计助理',
  pattern_maker: '版师',
  operator: '面料特供商',
  viewer: '查看者',
  merchant_owner: '管理员',
  merchant_operator: '面料特供商',
  merchant_viewer: '查看者',
  tenant_admin: '管理员',
  tenant_operator: '面料特供商',
  tenant_viewer: '查看者',
};

function getRoleLabel(role: string): string {
  return ROLE_LABEL_MAP[role] || role;
}

// 插件自己的租户ID（从 localStorage 读取）
const pluginTenantId = computed(() => localStorage.getItem('ai_design_tenant_id') || auth.tenantId)

// 当前工作室显示标签 —— 使用插件自己的租户上下文，不依赖全局 auth.tenantId
const currentStudioLabel = computed(() => {
  const pluginTenantId = localStorage.getItem('ai_design_tenant_id')
  const matched = pluginTenantId ? auth.tenants.find(t => String(t.tenantId) === pluginTenantId) : null
  if (matched) {
    return matched.tenantName;
  }
  return auth.tenantName || '工作室';
});

// 用于插件 TAB 标题的工作室名称（如：AI 设计助手 · 某某工作室）
const currentStudioTitle = computed(() => {
  const pluginTenantId = localStorage.getItem('ai_design_tenant_id')
  const matched = pluginTenantId ? auth.tenants.find(t => String(t.tenantId) === pluginTenantId) : null
  if (matched) {
    return matched.tenantName;
  }
  return auth.tenantName || '';
});

// 动态设置浏览器标签页标题
function updateDocumentTitle() {
  const studioPart = currentStudioTitle.value ? ` · ${currentStudioTitle.value}` : '';
  document.title = `AI 设计助手${studioPart}`;
}

watch(currentStudioTitle, () => {
  updateDocumentTitle();
}, { immediate: true });

const switching = ref(false);

const BOSS_ROLES = ['boss', 'merchant_owner', 'tenant_admin'];

function isBossRole(role: string): boolean {
  return BOSS_ROLES.includes(role);
}

async function handleDeleteTenant(tenant: UserTenantInfo) {
  try {
    await ElMessageBox.confirm(
      `确定要删除工作室「${tenant.tenantName}」吗？删除后所有成员将被移除，数据不可恢复。`,
      '删除工作室',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
    );
    const result = await auth.deleteTenant(tenant.tenantId);
    if (result.success) {
      ElMessage.success('工作室已删除');
      // 删除后刷新页面或跳转到身份选择页
      if (auth.tenants.length > 0) {
        window.location.reload();
      } else {
        router.replace('/plugins/ai-design-assistant/identity');
      }
    } else {
      ElMessage.error(result.message);
    }
  } catch {
    // 用户取消
  }
}

// 切换工作室 —— 只更新插件自己的租户上下文，不改变全局 auth
async function handleSwitchTenant(targetTenantId: number) {
  if (switching.value) return;
  if (String(targetTenantId) === localStorage.getItem('ai_design_tenant_id')) return;

  switching.value = true;
  try {
    const targetTenant = auth.tenants.find(t => t.tenantId === targetTenantId);
    if (!targetTenant) {
      ElMessage.error('工作室不存在');
      return;
    }

    // 更新插件自己的租户上下文（localStorage），不动全局 auth
    const roleLabel = getRoleLabel(targetTenant.role);
    localStorage.setItem('ai_design_tenant_id', String(targetTenantId));
    localStorage.setItem('ai_design_role', targetTenant.role);
    localStorage.setItem('ai_design_identity_prefix', `${targetTenant.tenantName}-${roleLabel}`);

    ElMessage.success(`已切换到「${targetTenant.tenantName}」`);

    // 根据新角色跳转到对应默认页（刷新数据）
    const newRole = targetTenant.role;
    if (newRole === 'boss' || newRole === 'merchant_owner' || newRole === 'tenant_admin') {
      router.push('/plugins/ai-design-assistant/board');
    } else {
      router.push('/plugins/ai-design-assistant/list');
    }
  } catch (e) {
    ElMessage.error('切换工作室失败');
  } finally {
    switching.value = false;
  }
}

// 下拉展开时加载工作室列表
function onDropdownVisibleChange(visible: boolean) {
  if (visible && !auth.tenantsLoaded) {
    auth.loadTenants();
  }
}

onMounted(() => {
  // 预加载工作室列表
  if (!auth.tenantsLoaded) {
    auth.loadTenants();
  }

  // 未选择身份时，重定向到身份选择页
  checkIdentity();

  // 设置初始标题
  updateDocumentTitle();
});

onUnmounted(() => {
  // 离开插件时恢复主应用标题（含租户企业名称）
  const name = auth.tenantName;
  document.title = name ? `濮院毛衫AI平台 + ${name}` : '濮院毛衫AI平台';
});

// 路由变化时也检查身份（处理直接访问子路由的情况）
watch(() => route.path, () => {
  checkIdentity();
});

function checkIdentity() {
  // 如果已经在身份选择页，不重定向
  if (route.path === '/plugins/ai-design-assistant/identity') return;
  
  const identityRole = localStorage.getItem('ai_design_role');
  if (!identityRole) {
    router.replace('/plugins/ai-design-assistant/identity');
  }
}

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
    roles: ['boss', 'merchant_owner'],
  },
  // 创建设计需求 - 设计师可创建
  {
    path: '/plugins/ai-design-assistant/create',
    name: '创建设计需求',
    icon: '✨',
    roles: ['designer'],
  },
  // 我的设计需求 - 仅设计师
  {
    path: '/plugins/ai-design-assistant/list',
    name: '我的设计需求',
    icon: '📋',
    roles: ['designer'],
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
    roles: ['boss', 'merchant_owner', 'designer', 'design_assistant', 'pattern_maker', 'operator', 'viewer'],
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
    roles: ['boss', 'merchant_owner'],
  },
  {
    path: '/plugins/ai-design-assistant/partners',
    name: '合作方管理',
    icon: '🤝',
    roles: ['boss', 'merchant_owner'],
  },
]

// 管理员类角色
const ADMIN_ROLES = ['boss', 'merchant_owner', 'tenant_admin']

// 根据当前角色过滤菜单
// 管理员：看到全部菜单（无权限的页面会显示引导邀请页）
// 普通角色：只看到自己有权限的 TAB
const visibleMenus = computed(() => {
  const currentRole = auth.currentRole
  if (!currentRole) {
    return pluginMenus
  }
  // 管理员角色 → 显示全部菜单，无权限的页面弹出引导
  if (ADMIN_ROLES.includes(currentRole)) {
    return pluginMenus
  }
  const filtered = pluginMenus.filter((menu) => {
    // 直接匹配
    if (menu.roles.includes(currentRole)) return true
    // merchant_operator / tenant_operator → operator 互认
    if ((currentRole === 'merchant_operator' || currentRole === 'tenant_operator') && menu.roles.includes('operator')) return true
    // merchant_viewer / tenant_viewer → viewer 互认
    if ((currentRole === 'merchant_viewer' || currentRole === 'tenant_viewer') && menu.roles.includes('viewer')) return true
    // merchant_editor → designer 互认
    if (currentRole === 'merchant_editor' && menu.roles.includes('designer')) return true
    return false
  })
  // 兜底：如果过滤后没有菜单，显示全部（避免白屏）
  return filtered.length > 0 ? filtered : pluginMenus
})

// 判断当前路由是否激活
function isActiveRoute(menuPath: string): boolean {
  if (menuPath === '/plugins/ai-design-assistant' || menuPath === '/plugins/ai-design-assistant/list') {
    return route.path === '/plugins/ai-design-assistant' || route.path === '/plugins/ai-design-assistant/list'
  }
  return route.path.startsWith(menuPath)
}
</script>

<style scoped>
.design-assistant-plugin {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: hsl(var(--background));
}

.plugin-header {
  flex-shrink: 0;
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

/* ====== 工作室切换按钮 ====== */
.studio-switch-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: hsl(var(--secondary));
  border: 1px solid hsl(var(--border));
  border-radius: calc(var(--radius) - 4px);
  color: hsl(var(--foreground));
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
  white-space: nowrap;
  max-width: 160px;
}

.studio-switch-btn:hover {
  background: hsl(var(--accent));
  border-color: hsl(var(--primary) / 0.3);
}

.studio-switch-icon {
  font-size: 14px;
  flex-shrink: 0;
}

.studio-switch-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.studio-switch-arrow {
  font-size: 12px;
  color: hsl(var(--muted-foreground));
  flex-shrink: 0;
}

/* ====== 下拉菜单项 ====== */
.dropdown-studio-name {
  font-weight: 500;
  margin-right: 8px;
}

.dropdown-studio-role {
  font-size: 12px;
  color: hsl(var(--muted-foreground));
  margin-left: auto;
}

.dropdown-check {
  margin-left: 8px;
  color: hsl(var(--primary));
}

.dropdown-delete-btn {
  margin-left: 8px;
  padding: 2px 6px;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: hsl(var(--muted-foreground));
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s;
  line-height: 1;
}

.dropdown-delete-btn:hover {
  background: hsl(var(--destructive) / 0.1);
  color: hsl(var(--destructive));
}

:deep(.el-dropdown-menu__item.is-active) {
  background: hsl(var(--primary) / 0.06);
}

.plugin-content {
  flex: 1;
  min-height: 0;
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
  overflow: hidden;
}

@media (max-width: 1024px) {
  .plugin-menu {
    display: none;
  }
}
</style>