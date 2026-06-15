<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore, type UserTenantInfo } from '@/stores/auth'
import { useDesignAssistantStore } from '../stores'
import type { IdentityInfo } from '../stores'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const auth = useAuthStore()
const store = useDesignAssistantStore()

// ========== 角色定义 ==========
interface RoleOption {
  role: string
  label: string
  icon: string
  description: string
}

const roleOptions: RoleOption[] = [
  { role: 'boss', label: '管理员', icon: '👑', description: '管理工作室成员、分配任务、查看所有数据' },
  { role: 'designer', label: '设计师', icon: '🎨', description: '创建设计需求、查看任务进度' },
  { role: 'design_assistant', label: '设计助理', icon: '📋', description: '处理设计需求、生成AI总结' },
  { role: 'pattern_maker', label: '版师', icon: '✂️', description: '接单打版任务、上传打版成果' },
  { role: 'operator', label: '面料特供商', icon: '📊', description: '管理面料库、查看需求数据' },
  { role: 'viewer', label: '查看者', icon: '👁️', description: '查看需求与任务进度' },
]

// 角色中文映射（包含后端返回的完整角色码）
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

function getRoleIcon(role: string): string {
  return roleOptions.find(r => r.role === role)?.icon || '👤'
}

// ========== 工作室数据（复用 auth store 的 tenants）==========
const loading = ref(true)
const loadError = ref('')

// 选中的工作室
const selectedStudio = ref<UserTenantInfo | null>(null)

// ========== 创建工作室 ==========
const showCreateForm = ref(false)
const creating = ref(false)
const newStudioName = ref('')
const createError = ref('')

// ========== 计算属性 ==========
const studios = computed(() => auth.tenants)
const hasStudios = computed(() => studios.value.length > 0)

const selectedRoleLabel = computed(() => {
  if (!selectedStudio.value) return ''
  return getRoleLabel(selectedStudio.value.role)
})

const selectedRoleIcon = computed(() => {
  if (!selectedStudio.value) return ''
  return getRoleIcon(selectedStudio.value.role)
})

const canConfirm = computed(() => {
  return selectedStudio.value !== null
})

const canCreate = computed(() => {
  return newStudioName.value.trim().length >= 2 && !creating.value
})

// ========== 加载工作室列表 ==========
onMounted(async () => {
  await loadStudios()
  // 仅在确认无工作室时才自动展开创建表单
  if (auth.tenants.length === 0 && !loadError.value) {
    showCreateForm.value = true
  }
})

async function loadStudios() {
  loading.value = true
  loadError.value = ''
  try {
    // 如果 auth store 已有数据（来自登录或 localStorage 恢复），优先展示，后台静默刷新
    if (auth.tenants.length > 0) {
      loading.value = false
      // 异步刷新，不阻塞 UI
      auth.loadTenants().catch(() => {})
      return
    }
    const list = await auth.loadTenants()
    if (list.length === 0) {
      loadError.value = ''
    }
  } catch (e) {
    console.error('加载工作室列表失败', e)
    loadError.value = '加载工作室列表失败，请检查网络连接后刷新重试'
  } finally {
    loading.value = false
  }
}

// ========== 确认进入 ==========
function confirmIdentity() {
  if (!selectedStudio.value) return

  const studio = selectedStudio.value
  const roleLabel = getRoleLabel(studio.role)

  const identityInfo: IdentityInfo = {
    tenantId: studio.tenantId,
    tenantName: studio.tenantName,
    role: studio.role,
    roleLabel,
    identityPrefix: `${studio.tenantName}-${roleLabel}`,
  }

  store.setIdentity(identityInfo)

  localStorage.setItem('ai_design_identity_prefix', identityInfo.identityPrefix)
  localStorage.setItem('ai_design_tenant_id', String(studio.tenantId))
  localStorage.setItem('ai_design_role', studio.role)

  // 不改变全局 auth store 的租户上下文——插件使用自己的 ai_design_tenant_id
  // 这样用户返回主框架时，主应用仍然使用企业租户，插件列表正常显示

  const BOSS_ROLES = ['boss', 'merchant_owner', 'tenant_admin'];
  if (BOSS_ROLES.includes(studio.role)) {
    router.push('/plugins/ai-design-assistant/board')
  } else {
    router.push('/plugins/ai-design-assistant/list')
  }
}

// ========== 创建工作室 ==========
async function createStudio() {
  if (!canCreate.value) return

  creating.value = true
  createError.value = ''

  try {
    const result = await auth.createTenant(newStudioName.value.trim())
    if (result.success && result.data) {
      selectedStudio.value = result.data
      showCreateForm.value = false
      newStudioName.value = ''
      confirmIdentity()
    } else {
      createError.value = result.message || '创建失败，请稍后重试'
    }
  } catch (e) {
    console.error('创建工作室失败', e)
    createError.value = '网络错误，创建失败'
  } finally {
    creating.value = false
  }
}

function retryLoad() {
  loadStudios()
}

// ========== 删除工作室 ==========
const BOSS_ROLES = ['boss', 'merchant_owner', 'tenant_admin'];

function isBossRole(role: string): boolean {
  return BOSS_ROLES.includes(role);
}

async function deleteStudio(studio: UserTenantInfo) {
  try {
    await ElMessageBox.confirm(
      `确定要删除工作室「${studio.tenantName}」吗？删除后所有成员将被移除，数据不可恢复。`,
      '删除工作室',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
    );
    const result = await auth.deleteTenant(studio.tenantId);
    if (result.success) {
      ElMessage.success('工作室已删除');
      // 如果删除的是当前选中的工作室，清除选中
      if (selectedStudio.value?.tenantId === studio.tenantId) {
        selectedStudio.value = null;
      }
      // 如果没有工作室了，自动展开创建表单
      if (!hasStudios.value) {
        showCreateForm.value = true
      }
    } else {
      ElMessage.error(result.message);
    }
  } catch {
    // 用户取消
  }
}
</script>

<template>
  <div class="identity-page">
    <div class="identity-card">
      <!-- 头部 -->
      <header class="identity-header">
        <div class="brand-row">
          <span class="brand-emoji">✨</span>
          <h1 class="brand-title">AI 设计助手</h1>
        </div>
        <p class="brand-desc">选择或创建一个工作室开始使用</p>
      </header>

      <!-- 加载中 -->
      <div v-if="loading" class="state-box">
        <div class="spinner"></div>
        <p class="state-text">加载工作室信息...</p>
      </div>

      <!-- 加载失败 -->
      <div v-else-if="loadError" class="state-box">
        <p class="error-text">{{ loadError }}</p>
        <button class="btn btn-primary" @click="retryLoad">重新加载</button>
      </div>

      <!-- 主内容：统一结构，有无工作室都展示列表+创建+确认 -->
      <template v-else>
        <div class="section">
          <h2 class="section-title">选择工作室</h2>

          <!-- 工作室列表 -->
          <div v-if="hasStudios" class="studio-list">
            <button
              v-for="studio in studios"
              :key="studio.tenantId"
              :class="['studio-item', { selected: selectedStudio?.tenantId === studio.tenantId }]"
              @click="selectedStudio = studio"
            >
              <span class="studio-avatar">
                {{ studio.tenantName?.charAt(0) || '工' }}
              </span>
              <span class="studio-body">
                <span class="studio-name">{{ studio.tenantName }}</span>
                <span class="studio-meta">
                  <span class="role-tag">{{ getRoleLabel(studio.role) }}</span>
                  <span v-if="studio.isDefault" class="default-tag">默认</span>
                </span>
              </span>
              <span v-if="selectedStudio?.tenantId === studio.tenantId" class="check-mark">✓</span>
              <span
                v-if="isBossRole(studio.role) && studios.length > 1"
                class="studio-delete-btn"
                @click.stop="deleteStudio(studio)"
                title="删除工作室"
              >✕</span>
            </button>
          </div>

          <!-- 没有工作室时的空状态提示 -->
          <div v-else class="empty-hint">
            <div class="empty-icon">🏗️</div>
            <p class="empty-text">还没有工作室，请先创建一个</p>
          </div>

          <!-- 创建新工作室入口 -->
          <div class="create-entry">
            <button
              v-if="!showCreateForm"
              class="btn btn-create-entry"
              @click="showCreateForm = true"
            >
              + 创建新工作室
            </button>

            <!-- 创建表单 -->
            <div v-if="showCreateForm" class="create-panel">
              <div class="create-panel-head">
                <span>🏗️ 创建新工作室</span>
              </div>
              <div class="create-panel-body">
                <input
                  v-model="newStudioName"
                  type="text"
                  class="input"
                  placeholder="请输入工作室名称（至少2个字）"
                  :disabled="creating"
                  @keyup.enter="createStudio"
                />
                <p v-if="createError" class="field-error">{{ createError }}</p>
                <div class="create-actions">
                  <button
                    v-if="hasStudios"
                    class="btn btn-secondary"
                    @click="showCreateForm = false; newStudioName = ''; createError = ''"
                    :disabled="creating"
                  >
                    取消
                  </button>
                  <button
                    class="btn btn-primary"
                    :disabled="!canCreate"
                    @click="createStudio"
                  >
                    <span v-if="creating" class="btn-spinner"></span>
                    {{ creating ? '创建中...' : '确认创建' }}
                  </button>
                </div>
              </div>
              <p class="create-panel-hint">
                💡 创建工作室后，你将自动成为该工作室的<strong>管理员</strong>。你可以邀请其他成员加入。
              </p>
            </div>
          </div>
        </div>

        <!-- 选中预览 -->
        <div v-if="selectedStudio" class="preview-box">
          <p class="preview-label">当前选中</p>
          <p class="preview-identity">
            <strong>{{ selectedStudio.tenantName }}</strong>
            <span class="preview-sep">·</span>
            <span>{{ selectedRoleIcon }} {{ selectedRoleLabel }}</span>
          </p>
          <p class="preview-hint" v-if="selectedStudio.role === 'boss' || selectedStudio.role === 'merchant_owner' || selectedStudio.role === 'tenant_admin'">
            👑 作为管理员，你将进入管理看板，查看工作室所有工作情况
          </p>
          <p class="preview-hint" v-else>
            🔒 你在此工作室中的身份为 <strong>{{ selectedRoleLabel }}</strong>，由管理员分配
          </p>
        </div>

        <!-- 操作按钮：始终展示"确认并进入" -->
        <div class="action-bar">
          <button class="btn btn-secondary" @click="router.push('/dashboard')">
            返回工作台
          </button>
          <button
            class="btn btn-primary"
            :disabled="!canConfirm"
            @click="confirmIdentity"
          >
            确认并进入
          </button>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
/* ====== 页面容器 ====== */
.identity-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  background: hsl(var(--background));
}

.identity-card {
  width: 100%;
  max-width: 560px;
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: var(--radius);
  padding: 32px;
  box-shadow: var(--shadow-md);
}

/* ====== 头部 ====== */
.identity-header {
  text-align: center;
  margin-bottom: 32px;
}

.brand-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-bottom: 8px;
}

.brand-emoji {
  font-size: 28px;
  line-height: 1;
}

.brand-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: hsl(var(--foreground));
}

.brand-desc {
  margin: 0;
  font-size: 14px;
  color: hsl(var(--muted-foreground));
}

/* ====== 通用状态 ====== */
.state-box {
  text-align: center;
  padding: 32px 0;
}

.state-text {
  color: hsl(var(--muted-foreground));
  font-size: 14px;
}

.error-text {
  color: hsl(var(--destructive));
  font-size: 14px;
  margin-bottom: 16px;
}

/* ====== Spinner ====== */
.spinner {
  width: 32px;
  height: 32px;
  border: 2px solid hsl(var(--border));
  border-top-color: hsl(var(--primary));
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 12px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ====== 区块 ====== */
.section {
  margin-bottom: 24px;
}

.section-title {
  margin: 0 0 12px;
  font-size: 15px;
  font-weight: 600;
  color: hsl(var(--foreground));
}

/* ====== 空状态提示 ====== */
.empty-hint {
  text-align: center;
  padding: 20px 0 8px;
}

.empty-icon {
  font-size: 40px;
  margin-bottom: 8px;
}

.empty-text {
  margin: 0;
  font-size: 14px;
  color: hsl(var(--muted-foreground));
}

/* ====== 工作室列表 ====== */
.studio-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.studio-item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 12px 14px;
  background: hsl(var(--secondary));
  border: 1px solid hsl(var(--border));
  border-radius: calc(var(--radius) - 4px);
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: left;
  color: hsl(var(--foreground));
}

.studio-item:hover {
  background: hsl(var(--accent));
  border-color: hsl(var(--primary) / 0.3);
}

.studio-item.selected {
  border-color: hsl(var(--primary));
  background: hsl(var(--primary) / 0.08);
}

.studio-avatar {
  width: 40px;
  height: 40px;
  border-radius: calc(var(--radius) - 4px);
  background: var(--gradient-accent);
  color: hsl(var(--primary-foreground));
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
  flex-shrink: 0;
}

.studio-body {
  flex: 1;
  min-width: 0;
}

.studio-name {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: hsl(var(--foreground));
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.studio-meta {
  display: flex;
  align-items: center;
  gap: 6px;
}

.role-tag {
  font-size: 12px;
  color: hsl(var(--muted-foreground));
}

.default-tag {
  font-size: 11px;
  padding: 1px 6px;
  background: hsl(var(--accent));
  color: hsl(var(--muted-foreground));
  border-radius: 9999px;
  font-weight: 500;
}

.check-mark {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: hsl(var(--primary));
  color: hsl(var(--primary-foreground));
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.studio-delete-btn {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: transparent;
  color: hsl(var(--muted-foreground));
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
  cursor: pointer;
  transition: all 0.15s;
  margin-left: 4px;
}

.studio-delete-btn:hover {
  background: hsl(var(--destructive) / 0.1);
  color: hsl(var(--destructive));
}

/* ====== 创建入口 ====== */
.create-entry {
  margin-top: 8px;
}

.btn-create-entry {
  width: 100%;
  padding: 10px;
  border: 1px dashed hsl(var(--border));
  border-radius: calc(var(--radius) - 4px);
  background: transparent;
  color: hsl(var(--muted-foreground));
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-create-entry:hover {
  border-color: hsl(var(--primary) / 0.4);
  color: hsl(var(--foreground));
  background: hsl(var(--accent));
}

/* ====== 创建面板 ====== */
.create-panel {
  background: hsl(var(--secondary));
  border: 1px solid hsl(var(--border));
  border-radius: calc(var(--radius) - 4px);
  overflow: hidden;
}

.create-panel-head {
  padding: 12px 14px 8px;
  font-size: 14px;
  font-weight: 600;
  color: hsl(var(--foreground));
}

.create-panel-body {
  padding: 0 14px 14px;
}

.create-panel-hint {
  margin: 0;
  padding: 10px 14px;
  font-size: 12px;
  color: hsl(var(--muted-foreground));
  border-top: 1px solid hsl(var(--border));
  line-height: 1.6;
}

.create-panel-hint strong {
  color: hsl(var(--foreground));
}

.create-actions {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

/* ====== 输入框 ====== */
.input {
  width: 100%;
  padding: 10px 12px;
  background: hsl(var(--input));
  border: 1px solid hsl(var(--border));
  border-radius: calc(var(--radius) - 4px);
  font-size: 14px;
  color: hsl(var(--foreground));
  outline: none;
  transition: border-color 0.2s ease;
  box-sizing: border-box;
  font-family: inherit;
}

.input::placeholder {
  color: hsl(var(--muted-foreground));
}

.input:focus {
  border-color: hsl(var(--primary) / 0.5);
  box-shadow: 0 0 0 2px hsl(var(--primary) / 0.1);
}

.input:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.field-error {
  margin: 8px 0 0;
  font-size: 13px;
  color: hsl(var(--destructive));
}

/* ====== 按钮 ====== */
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 20px;
  border: none;
  border-radius: calc(var(--radius) - 4px);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: inherit;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-primary {
  background: hsl(var(--primary));
  color: hsl(var(--primary-foreground));
}

.btn-primary:hover:not(:disabled) {
  background: hsl(var(--primary) / 0.9);
}

.btn-secondary {
  background: hsl(var(--secondary));
  color: hsl(var(--foreground));
  border: 1px solid hsl(var(--border));
}

.btn-secondary:hover:not(:disabled) {
  background: hsl(var(--accent));
}

.btn-block {
  width: 100%;
  padding: 12px;
  font-size: 15px;
}

.btn-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid hsl(var(--primary-foreground) / 0.3);
  border-top-color: hsl(var(--primary-foreground));
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

/* ====== 预览 ====== */
.preview-box {
  background: hsl(var(--secondary));
  border: 1px solid hsl(var(--border));
  border-radius: calc(var(--radius) - 4px);
  padding: 14px;
  margin-bottom: 20px;
}

.preview-label {
  margin: 0 0 6px;
  font-size: 12px;
  color: hsl(var(--muted-foreground));
}

.preview-identity {
  margin: 0 0 8px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 16px;
  color: hsl(var(--foreground));
}

.preview-identity strong {
  font-weight: 600;
}

.preview-sep {
  color: hsl(var(--muted-foreground));
}

.preview-hint {
  margin: 0;
  font-size: 13px;
  color: hsl(var(--muted-foreground));
  line-height: 1.5;
}

.preview-hint strong {
  color: hsl(var(--foreground));
}

/* ====== 操作栏 ====== */
.action-bar {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

/* ====== 响应式 ====== */
@media (max-width: 640px) {
  .identity-page {
    padding: 24px 16px;
  }

  .identity-card {
    padding: 24px 20px;
    border-radius: calc(var(--radius) - 2px);
  }

  .brand-title {
    font-size: 22px;
  }

  .action-bar {
    flex-direction: column-reverse;
  }

  .action-bar .btn {
    width: 100%;
  }

  .create-actions {
    flex-direction: column;
  }
}
</style>
