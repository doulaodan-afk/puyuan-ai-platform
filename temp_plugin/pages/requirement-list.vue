<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getRequirementList } from '../api'
import { useDesignAssistantStore } from '../stores'
import type { RequirementListItem } from '../api'

const router = useRouter()
const store = useDesignAssistantStore()

const requirements = ref<RequirementListItem[]>([])
const loading = ref(false)
const selectedStatus = ref('')

onMounted(async () => {
  await loadRequirements()
})

async function loadRequirements() {
  loading.value = true
  try {
    requirements.value = await getRequirementList({
      status: selectedStatus.value || undefined,
      page: 1,
      size: 50,
    })
    store.setRequirements(requirements.value)
  } catch (e: any) {
    console.error('加载需求列表失败', e)
  } finally {
    loading.value = false
  }
}

async function viewDetail(id: number) {
  router.push(`/plugins/ai-design-assistant/detail/${id}`)
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
</script>

<template>
  <div class="design-requirement-list page-container">
    <header class="header">
      <h1>我的设计需求</h1>
      <button @click="router.push('/plugins/ai-design-assistant/create')" class="btn btn-primary">
        + 创建新需求
      </button>
    </header>

    <div class="filters">
      <select v-model="selectedStatus" @change="loadRequirements" class="filter-select">
        <option value="">全部状态</option>
        <option value="draft">草稿</option>
        <option value="assistant_processing">助理处理中</option>
        <option value="released">已发布</option>
        <option value="completed">已完成</option>
        <option value="cancelled">已取消</option>
      </select>
    </div>

    <div v-if="loading" class="loading">加载中...</div>

    <div v-else-if="requirements.length === 0" class="empty">
      <p>暂无需求</p>
      <button @click="router.push('/plugins/ai-design-assistant/create')" class="btn btn-secondary">
        创建第一个需求
      </button>
    </div>

    <div v-else class="requirement-list">
      <div
        v-for="req in requirements"
        :key="req.id"
        @click="viewDetail(req.id)"
        class="requirement-card"
      >
        <div class="req-header">
          <h3>{{ req.title }}</h3>
          <span :class="['status-badge', getStatusClass(req.status)]">
            {{ getStatusLabel(req.status) }}
          </span>
        </div>
        <div class="req-info">
          <span>任务数: {{ req.taskCount }}</span>
          <span>Token: {{ req.totalTokenCost }}</span>
        </div>
        <div class="req-time">
          创建于 {{ new Date(req.createdAt).toLocaleString() }}
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.design-requirement-list {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header h1 {
  margin: 0;
  color: #333;
}

.filters {
  margin-bottom: 24px;
}

.filter-select {
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 14px;
  background: white;
  cursor: pointer;
}

.loading {
  text-align: center;
  padding: 40px;
  color: #666;
}

.empty {
  text-align: center;
  padding: 60px 20px;
  color: #666;
}

.empty p {
  margin-bottom: 16px;
}

.requirement-list {
  display: grid;
  gap: 16px;
}

.requirement-card {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.2s;
}

.requirement-card:hover {
  border-color: #2563eb;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.req-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.req-header h3 {
  margin: 0;
  color: #333;
  font-size: 16px;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-draft {
  background: #f3f4f6;
  color: #6b7280;
}

.status-processing {
  background: #fef3c7;
  color: #d97706;
}

.status-released {
  background: #dbeafe;
  color: #2563eb;
}

.status-completed {
  background: #d1fae5;
  color: #059669;
}

.status-cancelled {
  background: #fee2e2;
  color: #dc2626;
}

.req-info {
  display: flex;
  gap: 20px;
  margin-bottom: 8px;
  color: #666;
  font-size: 14px;
}

.req-time {
  color: #9ca3af;
  font-size: 13px;
}

.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary {
  background: #2563eb;
  color: white;
}

.btn-primary:hover {
  background: #1d4ed8;
}

.btn-secondary {
  background: #f3f4f6;
  color: #374151;
}

.btn-secondary:hover {
  background: #e5e7eb;
}
</style>