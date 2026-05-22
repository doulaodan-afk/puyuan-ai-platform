<template>
  <el-dropdown trigger="click" @command="handleCommand">
    <div class="tenant-switch">
      <span class="current-tenant">
        <span class="icon">🏢</span>
        <span class="name">{{ currentTenantName || '未选择工作室' }}</span>
        <span class="arrow">▼</span>
      </span>
    </div>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item
          v-for="tenant in userStore.tenants"
          :key="tenant.tenantId"
          :class="{ 'is-active': tenant.tenantId === currentTenantId }"
          :command="tenant.tenantId"
        >
          <span class="tenant-dropdown-item">
            <span class="name">{{ tenant.tenantName }}</span>
            <span class="role">{{ getRoleName(tenant.role) }}</span>
          </span>
          <span v-if="tenant.tenantId === currentTenantId" class="active-icon">✓</span>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '../stores/auth'
import { useRouter } from 'vue-router'

const userStore = useAuthStore()
const router = useRouter()

const currentTenantName = computed(() => {
  return userStore.currentTenant?.tenantName || '未选择工作室'
})

const currentTenantId = computed(() => userStore.currentTenantId)

const roleNames: Record<string, string> = {
  boss: '老板',
  designer: '设计师',
  design_assistant: '设计助理',
  pattern_maker: '版师',
  operator: '运营',
  viewer: '查看者',
}

function getRoleName(role: string): string {
  return roleNames[role] || role
}

function handleCommand(tenantId: number) {
  if (tenantId === currentTenantId.value) {
    return
  }

  userStore.switchTenant(tenantId)

  // 重新加载当前页面
  router.go(0)
}
</script>

<style scoped>
.tenant-switch {
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 8px;
  transition: background 0.2s;
}

.tenant-switch:hover {
  background: #f5f5f5;
}

.current-tenant {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #333;
}

.current-tenant .icon {
  font-size: 18px;
}

.current-tenant .name {
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.current-tenant .arrow {
  font-size: 10px;
  color: #999;
  margin-left: 4px;
}

.tenant-dropdown-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-width: 200px;
}

.tenant-dropdown-item .name {
  font-weight: 500;
}

.tenant-dropdown-item .role {
  font-size: 12px;
  color: #999;
  padding: 2px 8px;
  border-radius: 10px;
  background: #f0f0f0;
}

.is-active {
  background: #ecfdf5;
}

.is-active .active-icon {
  color: #10a37f;
  font-weight: bold;
}
</style>