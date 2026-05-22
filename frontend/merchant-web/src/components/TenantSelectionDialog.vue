<template>
  <el-dialog
    v-model="visible"
    title="选择工作室"
    width="500px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
  >
    <div class="tenant-selection">
      <p class="hint">您加入了多个工作室，请选择当前要使用的：</p>

      <div class="tenant-list">
        <div
          v-for="tenant in userStore.tenants"
          :key="tenant.tenantId"
          :class="['tenant-item', { selected: selectedTenantId === tenant.tenantId }]"
          @click="selectTenant(tenant.tenantId)"
        >
          <div class="tenant-icon">🏢</div>
          <div class="tenant-info">
            <div class="tenant-name">{{ tenant.tenantName }}</div>
            <div class="tenant-role">{{ getRoleName(tenant.role) }}</div>
          </div>
          <div v-if="selectedTenantId === tenant.tenantId" class="check-icon">✓</div>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button type="primary" @click="confirm" :disabled="!selectedTenantId">
        进入工作室
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useAuthStore } from '../stores/auth'

const userStore = useAuthStore()

const visible = ref(false)
const selectedTenantId = ref<number>(0)
const emit = defineEmits(['confirmed'])

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

function selectTenant(tenantId: number) {
  selectedTenantId.value = tenantId
}

function confirm() {
  if (selectedTenantId.value) {
    userStore.switchTenant(selectedTenantId.value)
    visible.value = false
    emit('confirmed', selectedTenantId.value)
  }
}

function show() {
  // 如果只有一个租户，直接确认
  if (userStore.tenants.length === 1) {
    selectedTenantId.value = userStore.tenants[0].tenantId
    userStore.switchTenant(selectedTenantId.value)
    emit('confirmed', selectedTenantId.value)
    return
  }

  // 如果之前选择过，使用上次的选择
  if (userStore.currentTenantId) {
    selectedTenantId.value = userStore.currentTenantId
  } else if (userStore.tenants.length > 0) {
    selectedTenantId.value = userStore.tenants[0].tenantId
  }

  visible.value = true
}

// 监听租户列表变化，自动显示选择对话框
watch(
  () => userStore.tenants.length,
  (newLength) => {
    if (newLength > 0) {
      show()
    }
  },
  { immediate: true }
)

defineExpose({ show })
</script>

<style scoped>
.tenant-selection {
  padding: 10px 0;
}

.hint {
  color: #666;
  margin-bottom: 20px;
  text-align: center;
}

.tenant-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tenant-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.tenant-item:hover {
  border-color: #3b82f6;
  background: #f8fafc;
}

.tenant-item.selected {
  border-color: #10a37f;
  background: #ecfdf5;
}

.tenant-icon {
  font-size: 32px;
}

.tenant-info {
  flex: 1;
}

.tenant-name {
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
}

.tenant-role {
  font-size: 14px;
  color: #6b7280;
}

.check-icon {
  font-size: 24px;
  color: #10a37f;
  font-weight: bold;
}
</style>