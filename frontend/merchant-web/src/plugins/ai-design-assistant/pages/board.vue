<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getRequirementList, getIdentityPrefix } from '../api'
import { useDesignAssistantStore } from '../stores'

const router = useRouter()
const store = useDesignAssistantStore()

// 身份前缀
const identityPrefix = computed(() => store.identity?.identityPrefix || getIdentityPrefix())

// 筛选状态
const statusFilter = ref('')

// 加载状态
const loading = ref(false)

// 需求列表
const requirements = computed(() => {
  if (statusFilter.value) {
    return store.requirements.filter(r => r.status === statusFilter.value)
  }
  return store.requirements
})

// 统计卡片
const stats = computed(() => {
  const list = store.requirements
  return {
    total: list.length,
    draft: list.filter(r => r.status === 'draft').length,
    processing: list.filter(r => r.status === 'assistant_processing').length,
    released: list.filter(r => r.status === 'released').length,
    completed: list.filter(r => r.status === 'completed').length,
    cancelled: list.filter(r => r.status === 'cancelled').length,
  }
})

// 总 Token 消耗
const totalTokens = computed(() => {
  return store.requirements.reduce((sum, r) => sum + r.totalTokenCost, 0)
})

onMounted(async () => {
  await fetchRequirements()
})

async function fetchRequirements() {
  loading.value = true
  try {
    const list = await getRequirementList({
      status: statusFilter.value || undefined,
      page: 1,
      size: 100,
    })
    store.setRequirements(list)
  } catch (e: any) {
    console.error('获取需求列表失败', e)
  } finally {
    loading.value = false
  }
}

function getStatusLabel(status: string) {
  const labels: Record<string, string> = {
    draft: '草稿',
    assistant_processing: '助理处理中',
    released: '已发布',
    completed: '已完成',
    cancelled: '已取消',
  }
  return labels[status] || status
}

function getProgressClass(progress: number) {
  if (progress < 30) return 'low'
  if (progress < 70) return 'medium'
  return 'high'
}

function getStatusClass(status: string) {
  const classes: Record<string, string> = {
    draft: 'status-draft',
    assistant_processing: 'status-processing',
    released: 'status-released',
    completed: 'status-completed',
    cancelled: 'status-cancelled',
  }
  return classes[status] || ''
}

function calculateProgress(requirement: any) {
  const tasks = requirement.tasks || []
  if (tasks.length === 0) return 0

  const completed = tasks.filter((t: any) =>
    ['done', 'delivered'].includes(t.status)
  ).length
  return Math.round((completed / tasks.length) * 100)
}

function viewDetail(requirement: any) {
  router.push(`/plugins/ai-design-assistant/detail/${requirement.id}`)
}

function createNew() {
  router.push('/plugins/ai-design-assistant/create')
}

function formatDate(dateStr: string) {
  return new Date(dateStr).toLocaleDateString('zh-CN')
}
</script>

<template>
  <div class="task-board-page page-container">
    <header class="header">
      <div class="header-title-row">
        <h1>👑 管理看板</h1>
        <span v-if="identityPrefix" class="identity-prefix-tag">
          {{ identityPrefix }}
        </span>
      </div>
      <div class="header-desc">查看工作室所有需求与任务进度</div>
    </header>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card total">
        <div class="stat-label">总需求</div>
        <div class="stat-value">{{ stats.total }}</div>
      </div>
      <div class="stat-card draft">
        <div class="stat-label">草稿</div>
        <div class="stat-value">{{ stats.draft }}</div>
      </div>
      <div class="stat-card processing">
        <div class="stat-label">处理中</div>
        <div class="stat-value">{{ stats.processing }}</div>
      </div>
      <div class="stat-card released">
        <div class="stat-label">已发布</div>
        <div class="stat-value">{{ stats.released }}</div>
      </div>
      <div class="stat-card completed">
        <div class="stat-label">已完成</div>
        <div class="stat-value">{{ stats.completed }}</div>
      </div>
      <div class="stat-card tokens">
        <div class="stat-label">Token 消耗</div>
        <div class="stat-value">{{ totalTokens }}</div>
      </div>
    </div>

    <!-- 筛选 -->
    <div class="filters">
      <button
        :class="['filter-btn', { active: statusFilter === '' }]"
        @click="statusFilter = ''; fetchRequirements()"
      >
        全部
      </button>
      <button
        :class="['filter-btn', { active: statusFilter === 'draft' }]"
        @click="statusFilter = 'draft'; fetchRequirements()"
      >
        草稿
      </button>
      <button
        :class="['filter-btn', { active: statusFilter === 'assistant_processing' }]"
        @click="statusFilter = 'assistant_processing'; fetchRequirements()"
      >
        助理处理中
      </button>
      <button
        :class="['filter-btn', { active: statusFilter === 'released' }]"
        @click="statusFilter = 'released'; fetchRequirements()"
      >
        已发布
      </button>
      <button
        :class="['filter-btn', { active: statusFilter === 'completed' }]"
        @click="statusFilter = 'completed'; fetchRequirements()"
      >
        已完成
      </button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>

    <div v-else-if="requirements.length === 0" class="empty">
      <div class="empty-icon">📊</div>
      <p>暂无需求</p>
      <button @click="createNew" class="btn btn-secondary">
        创建第一个需求
      </button>
    </div>

    <div v-else class="requirement-list">
      <div
        v-for="req in requirements"
        :key="req.id"
        @click="viewDetail(req)"
        class="requirement-card"
      >
        <div class="card-header">
          <div class="card-title">
            <h3>{{ req.title }}</h3>
          </div>
          <span :class="['status-badge', getStatusClass(req.status)]">
            {{ getStatusLabel(req.status) }}
          </span>
        </div>

        <div class="card-meta">
          <div class="meta-item">
            <span class="icon">📋</span>
            <span>{{ req.taskCount }} 个任务</span>
          </div>
          <div class="meta-item">
            <span class="icon">🔄</span>
            <span>{{ calculateProgress(req) }}% 完成</span>
          </div>
          <div class="meta-item">
            <span class="icon">💰</span>
            <span>{{ req.totalTokenCost }} Tokens</span>
          </div>
        </div>

        <div class="progress-bar">
          <div
            :class="['progress-fill', getProgressClass(calculateProgress(req))]"
            :style="{ width: calculateProgress(req) + '%' }"
          ></div>
        </div>

        <div class="card-footer">
          <span class="date">创建于 {{ formatDate(req.createdAt) }}</span>
          <button class="view-btn">查看详情 →</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.task-board-page {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 12px;
}

.header-title-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header h1 {
  margin: 0;
  color: hsl(var(--foreground));
  font-size: 24px;
}

.header-desc {
  font-size: 13px;
  color: hsl(var(--muted-foreground));
  margin-top: 4px;
}

.identity-prefix-tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  background: #eef2ff;
  border: 1px solid #c4b5fd;
  border-radius: 12px;
  font-size: 12px;
  color: #4338ca;
  font-weight: 500;
  font-family: 'SF Mono', Monaco, Consolas, monospace;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: 12px;
  padding: 20px;
  text-align: center;
}

.stat-card.total {
  background: linear-gradient(135deg, hsl(230 85% 65%) 0%, hsl(265 80% 67%) 100%);
  border: none;
}

.stat-card.draft {
  background: linear-gradient(135deg, hsl(var(--secondary)) 0%, hsl(var(--border)) 100%);
}

.stat-card.processing {
  background: linear-gradient(135deg, hsl(38 92% 55% / 0.15) 0%, hsl(45 97% 55%) 100%);
}

.stat-card.released {
  background: linear-gradient(135deg, hsl(217 91% 65% / 0.15) 0%, hsl(217 91% 65%) 100%);
}

.stat-card.completed {
  background: linear-gradient(135deg, hsl(142 71% 50% / 0.15) 0%, hsl(142 71% 50%) 100%);
}

.stat-card.tokens {
  background: linear-gradient(135deg, hsl(330 80% 60% / 0.15) 0%, hsl(330 80% 60%) 100%);
}

.stat-label {
  font-size: 13px;
  color: hsl(var(--foreground) / 0.6);
  margin-bottom: 8px;
}

.stat-card.total .stat-label {
  color: hsl(var(--foreground) / 0.9);
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: hsl(var(--foreground));
}

.stat-card.total .stat-value {
  color: hsl(var(--primary-foreground));
}

.filters {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.filter-btn {
  padding: 8px 16px;
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: 20px;
  font-size: 14px;
  color: hsl(var(--muted-foreground));
  cursor: pointer;
  transition: all 0.2s;
}

.filter-btn:hover {
  border-color: hsl(var(--border));
}

.filter-btn.active {
  background: hsl(160 84% 40%);
  border-color: hsl(160 84% 40%);
  color: hsl(var(--primary-foreground));
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
  margin-bottom: 12px;
}

.card-title h3 {
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

.status-processing {
  background: hsl(38 92% 55% / 0.15);
  color: hsl(38 92% 55%);
}

.status-released {
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
  align-items: center;
  gap: 6px;
}

.meta-item .icon {
  font-size: 16px;
}

.progress-bar {
  height: 6px;
  background: hsl(var(--secondary));
  border-radius: 3px;
  margin-bottom: 12px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.3s ease;
}

.progress-fill.low {
  background: hsl(45 97% 55%);
}

.progress-fill.medium {
  background: hsl(217 91% 65%);
}

.progress-fill.high {
  background: hsl(160 84% 45%);
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.date {
  font-size: 13px;
  color: hsl(var(--muted-foreground));
}

.view-btn {
  background: none;
  border: none;
  color: hsl(160 84% 40%);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  padding: 0;
}

.view-btn:hover {
  text-decoration: underline;
}

.btn {
  padding: 10px 24px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary {
  background: hsl(160 84% 40%);
  color: hsl(var(--primary-foreground));
}

.btn-primary:hover {
  background: hsl(160 84% 35%);
}

.btn-secondary {
  background: hsl(var(--secondary));
  color: hsl(var(--foreground));
}

.btn-secondary:hover {
  background: hsl(var(--border));
}
</style>