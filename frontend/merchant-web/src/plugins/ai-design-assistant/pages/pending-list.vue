<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getRequirementList } from '../api'
import { useDesignAssistantStore } from '../stores'

const router = useRouter()
const store = useDesignAssistantStore()

// 筛选状态
const activeTab = ref('pending')

// 加载状态
const loading = ref(false)

// 待处理需求列表
const pendingList = computed(() =>
  store.requirements.filter(r => r.status === 'assistant_processing')
)

// 处理中列表
const processingList = computed(() =>
  store.requirements.filter(r => r.status === 'released')
)

// 根据当前 tab 获取列表
const currentList = computed(() => {
  switch (activeTab.value) {
    case 'pending':
      return pendingList.value
    case 'processing':
      return processingList.value
    case 'all':
      return store.requirements.filter(r =>
        ['assistant_processing', 'released'].includes(r.status)
      )
    default:
      return pendingList.value
  }
})

// 待处理数量
const pendingCount = computed(() => pendingList.value.length)

onMounted(async () => {
  await fetchRequirements()
})

async function fetchRequirements() {
  loading.value = true
  try {
    const requirements = await getRequirementList({
      status: undefined,
      page: 1,
      size: 100,
    })
    store.setRequirements(requirements)
  } catch (e: any) {
    console.error('获取需求列表失败', e)
  } finally {
    loading.value = false
  }
}

function getStatusLabel(status: string) {
  const labels: Record<string, string> = {
    draft: '草稿',
    assistant_processing: '待处理',
    released: '已发布',
    completed: '已完成',
    cancelled: '已取消',
  }
  return labels[status] || status
}

function getStatusClass(status: string) {
  const classes: Record<string, string> = {
    draft: 'status-draft',
    assistant_processing: 'status-pending',
    released: 'status-processing',
    completed: 'status-completed',
    cancelled: 'status-cancelled',
  }
  return classes[status] || ''
}

function handleDetail(requirement: any) {
  router.push(`/plugins/ai-design-assistant/detail/${requirement.id}`)
}

function formatDate(dateStr: string) {
  return new Date(dateStr).toLocaleString('zh-CN')
}

// 查看需求对应的任务列表
function getTaskStatusText(requirement: any) {
  const tasks = requirement.tasks || []
  if (tasks.length === 0) return '无任务'

  const pending = tasks.filter((t: any) => t.status === 'draft').length
  const published = tasks.filter((t: any) => t.status !== 'draft').length

  if (pending > 0 && published === 0) return `草稿: ${pending} 个`
  if (pending === 0 && published > 0) return `已发布: ${published} 个`
  return `${pending} 个草稿, ${published} 个已发布`
}
</script>

<template>
  <div class="assistant-pending-page page-container">
    <header class="header">
      <h1>
        设计助理待办
        <span v-if="pendingCount > 0" class="badge">{{ pendingCount }}</span>
      </h1>
      <button @click="fetchRequirements" class="btn btn-secondary">
        刷新
      </button>
    </header>

    <div class="tabs">
      <button
        :class="['tab', { active: activeTab === 'pending' }]"
        @click="activeTab = 'pending'"
      >
        待处理 {{ pendingCount > 0 ? `(${pendingCount})` : '' }}
      </button>
      <button
        :class="['tab', { active: activeTab === 'processing' }]"
        @click="activeTab = 'processing'"
      >
        处理中
      </button>
      <button
        :class="['tab', { active: activeTab === 'all' }]"
        @click="activeTab = 'all'"
      >
        全部
      </button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>

    <div v-else-if="currentList.length === 0" class="empty">
      <div class="empty-icon">📋</div>
      <p>{{ activeTab === 'pending' ? '暂无待处理需求' : '暂无需求' }}</p>
    </div>

    <div v-else class="requirement-list">
      <div
        v-for="req in currentList"
        :key="req.id"
        @click="handleDetail(req)"
        class="requirement-card"
      >
        <div class="card-header">
          <div class="requirement-title">
            <h3>{{ req.title }}</h3>
          </div>
          <span :class="['status-badge', getStatusClass(req.status)]">
            {{ getStatusLabel(req.status) }}
          </span>
        </div>

        <div class="card-meta">
          <div class="meta-item">
            <span class="meta-label">任务数:</span>
            <span>{{ req.taskCount }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">Token:</span>
            <span>{{ req.totalTokenCost }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">任务状态:</span>
            <span>{{ getTaskStatusText(req) }}</span>
          </div>
        </div>

        <div class="card-time">
          创建于 {{ formatDate(req.createdAt) }}
        </div>

        <div class="card-actions">
          <button class="btn btn-primary">处理</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.assistant-pending-page {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header h1 {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  color: hsl(var(--foreground));
  font-size: 24px;
}

.badge {
  background: hsl(var(--destructive));
  color: hsl(var(--primary-foreground));
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
}

.tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  border-bottom: 1px solid hsl(var(--border));
}

.tab {
  padding: 10px 16px;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  color: hsl(var(--muted-foreground));
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.tab.active {
  color: hsl(160 84% 40%);
  border-bottom-color: hsl(160 84% 40%);
}

.tab:hover {
  color: hsl(160 84% 40%);
}

.loading {
  text-align: center;
  padding: 40px;
  color: hsl(var(--muted-foreground));
}

.empty {
  text-align: center;
  padding: 80px 20px;
  color: hsl(var(--muted-foreground));
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.requirement-list {
  display: grid;
  gap: 16px;
}

.requirement-card {
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.2s;
}

.requirement-card:hover {
  border-color: hsl(var(--border));
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.requirement-title h3 {
  margin: 0;
  color: hsl(var(--foreground));
  font-size: 16px;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-draft {
  background: hsl(var(--secondary));
  color: hsl(var(--muted-foreground));
}

.status-pending {
  background: hsl(38 92% 55% / 0.15);
  color: hsl(38 92% 55%);
}

.status-processing {
  background: hsl(217 91% 65% / 0.15);
  color: hsl(217 91% 65%);
}

.status-completed {
  background: hsl(142 71% 50% / 0.15);
  color: hsl(142 71% 50%);
}

.status-cancelled {
  background: hsl(var(--destructive) / 0.15);
  color: hsl(var(--destructive));
}

.card-meta {
  display: flex;
  gap: 24px;
  margin-bottom: 12px;
  font-size: 14px;
  color: hsl(var(--muted-foreground));
}

.meta-item {
  display: flex;
  gap: 6px;
}

.meta-label {
  color: hsl(var(--muted-foreground));
}

.card-time {
  font-size: 13px;
  color: hsl(var(--muted-foreground));
  margin-bottom: 12px;
}

.card-actions {
  display: flex;
  gap: 8px;
}

.btn {
  padding: 8px 20px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.btn-primary {
  background: hsl(160 84% 40%);
  color: hsl(var(--primary-foreground));
}

.btn-primary:hover:not(:disabled) {
  background: hsl(160 84% 35%);
}

.btn-secondary {
  background: hsl(var(--secondary));
  color: hsl(var(--foreground));
}

.btn-secondary:hover:not(:disabled) {
  background: hsl(var(--border));
}
</style>