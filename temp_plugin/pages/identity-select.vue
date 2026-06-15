<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useDesignAssistantStore } from '../stores'
import type { IdentityInfo } from '../stores'

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
  { role: 'operator', label: '运营', icon: '📊', description: '查看需求数据、管理面料库' },
  { role: 'viewer', label: '查看者', icon: '👁️', description: '查看需求与任务进度' },
]

function getRoleLabel(role: string): string {
  return roleOptions.find(r => r.role === role)?.label || role
}

function getRoleIcon(role: string): string {
  return roleOptions.find(r => r.role === role)?.icon || '👤'
}

// ========== 工作室数据 ==========
interface StudioInfo {
  tenantId: number
  tenantName: string
  tenantCode: string
  role: string
  isDefault: boolean
}

const studios = ref<StudioInfo[]>([])
const loading = ref(true)
const loadError = ref('')

// 选中的工作室
const selectedStudio = ref<StudioInfo | null>(null)

// ========== 创建工作室 ==========
const showCreateForm = ref(false)
const creating = ref(false)
const newStudioName = ref('')
const createError = ref('')
const createSuccess = ref('')

// ========== 计算属性 ==========
const hasStudios = computed(() => studios.value.length > 0)

const selectedRoleLabel = computed(() => {
  if (!selectedStudio.value) return ''
  return getRoleLabel(selectedStudio.value.role)
})

const selectedRoleIcon = computed(() => {
  if (!selectedStudio.value) return ''
  return getRoleIcon(selectedStudio.value.role)
})

const identityPrefix = computed(() => {
  if (!selectedStudio.value) return ''
  return `${selectedStudio.value.tenantName}-${getRoleLabel(selectedStudio.value.role)}`
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
})

async function loadStudios() {
  loading.value = true
  loadError.value = ''
  try {
    // 从 accessToken 解析 userId（token 格式: token-{userId}-{tenantId}）
    const tokenRaw = auth.accessToken.replace('Bearer ', '')
    const tokenParts = tokenRaw.split('-')
    const userId = tokenParts.length >= 2 ? tokenParts[1] : ''

    const response = await fetch('/api/tenant/user/tenants', {
      headers: {
        'Authorization': `Bearer ${auth.accessToken}`,
        'X-User-Id': userId,
        'X-Request-Id': crypto.randomUUID(),
      },
    })
    const result = await response.json()
    if (result.code === 0 && result.data && result.data.length > 0) {
      studios.value = result.data.map((s: any) => ({
        tenantId: s.tenantId || s.tenant_id,
        tenantName: s.tenantName || s.tenant_name,
        tenantCode: s.tenantCode || s.tenant_code,
        role: s.role || s.role_code,
        isDefault: s.isDefault || s.is_default || false,
      }))
    } else {
      studios.value = []
    }
  } catch (e) {
    console.error('加载工作室列表失败', e)
    loadError.value = '加载工作室列表失败，请检查网络连接后刷新重试'
    studios.value = []
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

  if (studio.role === 'boss') {
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
  createSuccess.value = ''

  try {
    // 从 accessToken 解析 userId（token 格式: token-{userId}-{tenantId}）
    const tokenRaw = auth.accessToken.replace('Bearer ', '')
    const tokenParts = tokenRaw.split('-')
    const userId = tokenParts.length >= 2 ? tokenParts[1] : ''

    const response = await fetch('/api/tenant/create', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${auth.accessToken}`,
        'X-User-Id': userId,
        'X-Request-Id': crypto.randomUUID(),
      },
      body: JSON.stringify({
        tenantName: newStudioName.value.trim(),
      }),
    })

    const result = await response.json()
    if (result.code === 0 && result.data) {
      createSuccess.value = '工作室创建成功！'

      const newStudio: StudioInfo = {
        tenantId: result.data.tenantId || result.data.tenant_id,
        tenantName: result.data.tenantName || result.data.tenant_name || newStudioName.value.trim(),
        tenantCode: result.data.tenantCode || result.data.tenant_code || '',
        role: 'boss',
        isDefault: false,
      }
      studios.value.push(newStudio)
      selectedStudio.value = newStudio

      setTimeout(() => {
        showCreateForm.value = false
        newStudioName.value = ''
        createSuccess.value = ''
      }, 1500)
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

      <!-- 有工作室：选择工作室 -->
      <template v-else-if="hasStudios">
        <div class="section">
          <h2 class="section-title">选择工作室</h2>
          <div class="studio-list">
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
            </button>
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
                <p v-if="createSuccess" class="field-success">{{ createSuccess }}</p>
                <div class="create-actions">
                  <button
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
          <p class="preview-hint" v-if="selectedStudio.role === 'boss'">
            👑 作为管理员，你将进入管理看板，查看工作室所有工作情况
          </p>
          <p class="preview-hint" v-else>
            🔒 你在此工作室中的身份为 <strong>{{ selectedRoleLabel }}</strong>，由管理员分配
          </p>
        </div>

        <!-- 操作按钮 -->
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

      <!-- 没有工作室 -->
      <template v-else>
        <div class="section empty-section">
          <div class="empty-icon">🏗️</div>
          <h2 class="empty-title">还没有工作室</h2>
          <p class="empty-desc">
            你需要先创建或加入一个工作室，才能使用 AI 设计助手。
          </p>

          <!-- 创建表单 -->
          <div class="create-panel standalone">
            <div class="create-panel-head">
              <span>创建你的第一个工作室</span>
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
              <p v-if="createSuccess" class="field-success">{{ createSuccess }}</p>
              <button
                class="btn btn-primary btn-block"
                :disabled="!canCreate"
                @click="createStudio"
              >
                <span v-if="creating" class="btn-spinner"></span>
                {{ creating ? '创建中...' : '创建工作室' }}
              </button>
            </div>
            <p class="create-panel-hint">
              💡 创建工作室后，你将自动成为该工作室的<strong>管理员</strong>。你可以邀请其他成员加入，为每位成员分配唯一的角色。
            </p>
          </div>

          <div class="invite-notice">
            <p>如果你已经被其他工作室邀请，请联系管理员确认后刷新页面即可看到。</p>
          </div>
        </div>

        <div class="action-bar">
          <button class="btn btn-secondary" @click="router.push('/dashboard')">
            返回工作台
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

.create-panel.standalone {
  margin-bottom: 20px;
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

.field-success {
  margin: 8px 0 0;
  font-size: 13px;
  color: hsl(160 84% 40%);
  font-weight: 500;
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

/* ====== 空状态 ====== */
.empty-section {
  text-align: center;
  padding: 12px 0 0;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.empty-title {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 700;
  color: hsl(var(--foreground));
}

.empty-desc {
  margin: 0 0 24px;
  font-size: 14px;
  color: hsl(var(--muted-foreground));
  line-height: 1.6;
}

.invite-notice {
  margin-top: 16px;
  padding: 12px 14px;
  background: hsl(var(--secondary));
  border: 1px solid hsl(var(--border));
  border-radius: calc(var(--radius) - 4px);
  font-size: 13px;
  color: hsl(var(--muted-foreground));
  line-height: 1.5;
  text-align: left;
}

.invite-notice p {
  margin: 0;
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
