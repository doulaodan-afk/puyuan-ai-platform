<template>
  <el-dropdown trigger="click" @command="handleCommand">
    <div class="tenant-switch">
      <span class="icon">🏢</span>
      <span class="name">{{ currentTenantName }}</span>
      <span class="arrow">▼</span>
    </div>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item
          v-for="tenant in auth.tenants"
          :key="tenant.tenantId"
          :class="{ 'is-active': tenant.tenantId === auth.currentTenantId }"
          :command="tenant.tenantId"
        >
          <span class="tenant-dropdown-item">
            <span class="name">{{ tenant.tenantName }}</span>
            <span class="role">{{ getRoleLabel(tenant.role) }}</span>
          </span>
          <span v-if="tenant.tenantId === auth.currentTenantId" class="active-icon">✓</span>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore, getRoleLabel } from '../stores/auth'
import { ElMessage } from 'element-plus'

const auth = useAuthStore()

const currentTenantName = computed(() => {
  return auth.tenantName || '未选择工作室'
})

async function handleCommand(tenantId: number) {
  if (tenantId === auth.currentTenantId) return
  const result = await auth.switchTenant(tenantId)
  if (result.success) {
    ElMessage.success('已切换到 ' + (auth.tenantName || '工作室'))
    window.location.reload()
  } else {
    ElMessage.error(result.message || '切换失败')
  }
}
</script>

<style scoped>
.tenant-switch {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  padding: 4px 12px;
  border-radius: var(--radius);
  border: 1px solid hsl(var(--border));
  transition: all 0.15s ease;
  background: hsl(var(--secondary));
  color: hsl(var(--foreground));
  font-size: 14px;
  font-weight: 500;
  max-width: 200px;
}

.tenant-switch:hover {
  background: hsl(var(--accent));
  border-color: hsl(var(--primary) / 0.3);
}

.tenant-switch:active,
.tenant-switch:focus {
  background: hsl(var(--primary) / 0.08);
  border-color: hsl(var(--primary));
}

.icon {
  font-size: 16px;
}

.name {
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.arrow {
  font-size: 10px;
  color: hsl(var(--muted-foreground));
}

.tenant-dropdown-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  min-width: 180px;
}

.tenant-dropdown-item .name {
  font-weight: 600;
  color: hsl(var(--foreground));
}

.tenant-dropdown-item .role {
  font-size: 12px;
  color: hsl(var(--muted-foreground));
  margin-left: 8px;
}

.is-active {
  background: hsl(var(--primary) / 0.08);
}

.active-icon {
  color: hsl(var(--primary));
  font-weight: bold;
}
</style>