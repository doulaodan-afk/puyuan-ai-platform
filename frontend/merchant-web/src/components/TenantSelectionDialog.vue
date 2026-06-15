<template>
  <el-dialog
    v-model="visible"
    title="选择工作室"
    width="500px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
    class="tenant-selection-dialog"
  >
    <div class="tenant-selection">
      <p class="hint">您加入了多个工作室，请选择当前要使用的：</p>

      <div class="tenant-list">
        <div
          v-for="tenant in auth.tenants"
          :key="tenant.tenantId"
          :class="['tenant-item', { selected: selectedTenantId === tenant.tenantId }]"
          @click="selectTenant(tenant.tenantId)"
        >
          <div class="tenant-icon">🏢</div>
          <div class="tenant-info">
            <div class="tenant-name">{{ tenant.tenantName }}</div>
            <div class="tenant-role">{{ getRoleLabel(tenant.role) }}</div>
          </div>
          <div v-if="selectedTenantId === tenant.tenantId" class="check-icon">✓</div>
        </div>
      </div>

      <div class="create-section">
        <button v-if="!showCreateForm" class="create-btn" @click="showCreateForm = true">
          + 创建新工作室
        </button>
        <div v-if="showCreateForm" class="create-form">
          <input
            v-model="newStudioName"
            type="text"
            placeholder="请输入工作室名称（至少2个字）"
            :disabled="creating"
            @keyup.enter="handleCreateTenant"
          />
          <p v-if="createError" class="field-error">{{ createError }}</p>
          <p v-if="createSuccess" class="field-success">{{ createSuccess }}</p>
          <div class="form-actions">
            <button class="cancel-btn" @click="cancelCreate" :disabled="creating">取消</button>
            <button class="confirm-btn" :disabled="!canCreate || creating" @click="handleCreateTenant">
              {{ creating ? '创建中...' : '确认创建' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button type="primary" @click="confirm" :disabled="!selectedTenantId" :loading="switching">
        进入工作室
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useAuthStore, getRoleLabel } from '../stores/auth'

const emit = defineEmits<{
  confirmed: [tenantId: number]
}>()

const auth = useAuthStore()

const visible = ref(false)
const selectedTenantId = ref<number>(0)
const switching = ref(false)
const showCreateForm = ref(false)
const creating = ref(false)
const newStudioName = ref('')
const createError = ref('')
const createSuccess = ref('')
const canCreate = computed(() => newStudioName.value.trim().length >= 2 && !creating.value)

function selectTenant(tenantId: number) {
  selectedTenantId.value = tenantId
}

async function confirm() {
  if (!selectedTenantId.value) return
  switching.value = true
  try {
    const result = await auth.switchTenant(selectedTenantId.value)
    if (result.success) {
      visible.value = false
      emit('confirmed', selectedTenantId.value)
    } else {
      createError.value = result.message || '切换失败'
    }
  } finally {
    switching.value = false
  }
}

async function handleCreateTenant() {
  if (!canCreate.value) return
  creating.value = true
  createError.value = ''
  createSuccess.value = ''
  try {
    const result = await auth.createTenant(newStudioName.value.trim())
    if (result.success) {
      createSuccess.value = '工作室创建成功！'
      selectedTenantId.value = result.data!.tenantId
      setTimeout(() => confirm(), 500)
    } else {
      createError.value = result.message || '创建失败'
    }
  } catch (e) {
    console.error('创建工作室失败', e)
    createError.value = '创建工作室失败'
  } finally {
    creating.value = false
  }
}

function cancelCreate() {
  showCreateForm.value = false
  newStudioName.value = ''
  createError.value = ''
}

function show() {
  if (auth.tenants.length === 1) {
    selectedTenantId.value = auth.tenants[0].tenantId
    confirm()
    return
  }
  selectedTenantId.value = auth.currentTenantId || (auth.tenants[0]?.tenantId ?? 0)
  visible.value = true
}

defineExpose({ show })
</script>

<style scoped>
.tenant-selection {
  padding: 10px 0;
}

.hint {
  color: hsl(var(--muted-foreground));
  margin-bottom: 20px;
  text-align: center;
  font-size: 14px;
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
  border: 2px solid hsl(var(--border));
  border-radius: var(--radius);
  cursor: pointer;
  transition: all 0.2s;
}

.tenant-item:hover {
  border-color: hsl(var(--primary) / 0.3);
  background: hsl(var(--accent));
}

.tenant-item.selected {
  border-color: hsl(var(--primary));
  background: hsl(var(--primary) / 0.08);
}

.tenant-icon {
  font-size: 32px;
}

.tenant-info {
  flex: 1;
}

.tenant-name {
  font-weight: 600;
  color: hsl(var(--foreground));
  margin-bottom: 4px;
}

.tenant-role {
  font-size: 14px;
  color: hsl(var(--muted-foreground));
}

.check-icon {
  font-size: 24px;
  color: hsl(var(--primary));
  font-weight: bold;
}

.create-section {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed hsl(var(--border));
}

.create-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  padding: 10px;
  background: transparent;
  border: 1px dashed hsl(var(--border));
  border-radius: var(--radius);
  color: hsl(var(--muted-foreground));
  font-size: 14px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.create-btn:hover {
  border-color: hsl(var(--primary) / 0.4);
  color: hsl(var(--primary));
  background: hsl(var(--primary) / 0.05);
}

.create-form {
  margin-top: 8px;
  padding: 12px;
  background: hsl(var(--secondary));
  border: 1px solid hsl(var(--border));
  border-radius: var(--radius);
}

.create-form input {
  width: 100%;
  padding: 8px 10px;
  background: hsl(var(--input));
  border: 1px solid hsl(var(--border));
  border-radius: calc(var(--radius) - 4px);
  font-size: 14px;
  color: hsl(var(--foreground));
}

.create-form input::placeholder {
  color: hsl(var(--muted-foreground));
}

.create-form input:focus {
  border-color: hsl(var(--primary) / 0.5);
  box-shadow: 0 0 0 2px hsl(var(--primary) / 0.1);
}

.form-actions {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.cancel-btn {
  padding: 8px 16px;
  background: hsl(var(--secondary));
  color: hsl(var(--foreground));
  border: 1px solid hsl(var(--border));
  border-radius: var(--radius);
  cursor: pointer;
  font-size: 14px;
}

.confirm-btn {
  padding: 8px 16px;
  background: hsl(var(--primary));
  color: hsl(var(--primary-foreground));
  border: none;
  border-radius: var(--radius);
  cursor: pointer;
  font-size: 14px;
}

.field-error {
  font-size: 13px;
  color: hsl(var(--destructive));
  margin-top: 6px;
}

.field-success {
  font-size: 13px;
  color: hsl(160 84% 40%);
  margin-top: 6px;
}
</style>