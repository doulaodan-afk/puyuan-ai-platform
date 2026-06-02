<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  getMyTasks,
  updateTaskStatus,
  shipTask,
  uploadTaskResult,
  type TaskInfo,
} from '../api/design-assistant'
import { useDesignAssistantStore } from '../stores/designAssistant'

const router = useRouter()
const store = useDesignAssistantStore()

// 筛选状态
const activeTab = ref('pending')
const taskTypeFilter = ref<string>('')

// 发货弹窗
const shipDialogVisible = ref(false)
const currentTask = ref<TaskInfo | null>(null)
const shipForm = ref({
  logisticsCompany: '',
  logisticsTrackingNo: '',
  offlineLogisticsNote: '',
})

// 拒绝原因弹窗
const rejectDialogVisible = ref(false)
const rejectReason = ref('')

// 上传结果弹窗
const resultDialogVisible = ref(false)
const resultUrl = ref('')

// 加载状态
const loading = ref(false)

// 任务列表
const tasks = computed(() => {
  let filtered = store.myTasks

  if (taskTypeFilter.value) {
    filtered = filtered.filter(t => t.taskType === taskTypeFilter.value)
  }

  switch (activeTab.value) {
    case 'pending':
      return filtered.filter(t => t.status === 'pending')
    case 'processing':
      return filtered.filter(t =>
        ['accepted', 'shipped'].includes(t.status)
      )
    case 'completed':
      return filtered.filter(t =>
        ['done', 'delivered'].includes(t.status)
      )
    case 'rejected':
      return filtered.filter(t => t.status === 'rejected')
    default:
      return filtered
  }
})

// 待处理任务数
const pendingCount = computed(() =>
  store.myTasks.filter(t => t.status === 'pending').length
)

onMounted(async () => {
  await fetchTasks()
})

async function fetchTasks() {
  loading.value = true
  try {
    const response = await getMyTasks({
      status: undefined,
      taskType: taskTypeFilter.value || undefined,
      page: 1,
      size: 100,
    })
    store.setMyTasks(response.tasks, response.total)
  } catch (e: any) {
    console.error('获取任务失败', e)
  } finally {
    loading.value = false
  }
}

function getTaskTypeLabel(type: string) {
  return type === 'fabric' ? '面料' : '打版'
}

function getTaskTypeClass(type: string) {
  return type === 'fabric' ? 'task-fabric' : 'task-pattern'
}

function getStatusLabel(status: string) {
  const labels: Record<string, string> = {
    draft: '草稿',
    pending: '待处理',
    accepted: '已接受',
    shipped: '已发货',
    delivered: '已送达',
    rejected: '已拒绝',
    done: '已完成',
    cancelled: '已取消',
  }
  return labels[status] || status
}

function getStatusClass(status: string) {
  const classes: Record<string, string> = {
    draft: 'status-draft',
    pending: 'status-pending',
    accepted: 'status-accepted',
    shipped: 'status-shipped',
    delivered: 'status-delivered',
    rejected: 'status-rejected',
    done: 'status-done',
    cancelled: 'status-cancelled',
  }
  return classes[status] || ''
}

async function acceptTask(task: TaskInfo) {
  if (!task.canAccept) {
    alert(task.cannotAcceptReason || '无法接受此任务')
    return
  }

  if (confirm('确认接受此任务？')) {
    try {
      await updateTaskStatus({
        taskId: task.id,
        status: 'accepted',
      })
      store.updateTaskStatus(task.id, 'accepted')
      alert('任务已接受')
    } catch (e: any) {
      alert('操作失败: ' + e.message)
    }
  }
}

function openRejectDialog(task: TaskInfo) {
  currentTask.value = task
  rejectReason.value = ''
  rejectDialogVisible.value = true
}

async function submitReject() {
  if (!currentTask.value || !rejectReason.value.trim()) {
    alert('请填写拒绝原因')
    return
  }

  try {
    await updateTaskStatus({
      taskId: currentTask.value.id,
      status: 'rejected',
      rejectReason: rejectReason.value,
    })
    store.updateTaskStatus(currentTask.value.id, 'rejected')
    rejectDialogVisible.value = false
    alert('任务已拒绝')
    await fetchTasks()
  } catch (e: any) {
    alert('操作失败: ' + e.message)
  }
}

function openShipDialog(task: TaskInfo) {
  currentTask.value = task
  shipForm.value = {
    logisticsCompany: '',
    logisticsTrackingNo: '',
    offlineLogisticsNote: '',
  }
  shipDialogVisible.value = true
}

async function submitShip() {
  if (!currentTask.value) return

  const { logisticsCompany, logisticsTrackingNo, offlineLogisticsNote } = shipForm.value

  if (!logisticsCompany && !logisticsTrackingNo && !offlineLogisticsNote) {
    alert('请至少填写一种物流信息')
    return
  }

  try {
    await shipTask({
      taskId: currentTask.value.id,
      logisticsCompany: logisticsCompany || undefined,
      logisticsTrackingNo: logisticsTrackingNo || undefined,
      offlineLogisticsNote: offlineLogisticsNote || undefined,
    })
    shipDialogVisible.value = false
    alert('发货信息已提交')
    await fetchTasks()
  } catch (e: any) {
    alert('操作失败: ' + e.message)
  }
}

function openResultDialog(task: TaskInfo) {
  currentTask.value = task
  resultUrl.value = ''
  resultDialogVisible.value = true
}

async function submitResult() {
  if (!currentTask.value || !resultUrl.value.trim()) {
    alert('请填写结果文件URL')
    return
  }

  try {
    await uploadTaskResult({
      taskId: currentTask.value.id,
      resultUrl: resultUrl.value,
    })
    store.updateTaskStatus(currentTask.value.id, 'done')
    resultDialogVisible.value = false
    alert('结果已上传')
    await fetchTasks()
  } catch (e: any) {
    alert('操作失败: ' + e.message)
  }
}

function viewDetail(task: TaskInfo) {
  router.push(`/task/${task.id}`)
}

function formatDate(dateStr: string | null) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}
</script>

<template>
  <div class="my-tasks-page page-container">
    <header class="header">
      <h1>我的任务 <span v-if="pendingCount > 0" class="badge">{{ pendingCount }}</span></h1>
      <div class="filters">
        <select v-model="taskTypeFilter" @change="fetchTasks" class="filter-select">
          <option value="">全部类型</option>
          <option value="fabric">面料任务</option>
          <option value="pattern">打版任务</option>
        </select>
      </div>
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
        进行中
      </button>
      <button
        :class="['tab', { active: activeTab === 'completed' }]"
        @click="activeTab = 'completed'"
      >
        已完成
      </button>
      <button
        :class="['tab', { active: activeTab === 'rejected' }]"
        @click="activeTab = 'rejected'"
      >
        已拒绝
      </button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>

    <div v-else-if="tasks.length === 0" class="empty">
      <p>{{ activeTab === 'pending' ? '暂无待处理任务' : '暂无任务' }}</p>
    </div>

    <div v-else class="task-list">
      <div v-for="task in tasks" :key="task.id" class="task-card">
        <div class="task-header">
          <div class="task-title">
            <span :class="['task-type', getTaskTypeClass(task.taskType)]">
              {{ getTaskTypeLabel(task.taskType) }}
            </span>
            <span>{{ task.requirementTitle }}</span>
          </div>
          <span :class="['status-badge', getStatusClass(task.status)]">
            {{ getStatusLabel(task.status) }}
          </span>
        </div>

        <div class="task-content">
          <pre v-if="task.content">{{ JSON.stringify(task.content, null, 2) }}</pre>
          <p v-else>无详细内容</p>
        </div>

        <div class="task-meta">
          <div v-if="task.deadline">
            截止: {{ formatDate(task.deadline) }}
          </div>
          <div>创建于 {{ formatDate(task.createdAt) }}</div>
        </div>

        <!-- 物流信息（仅面料任务） -->
        <div v-if="task.taskType === 'fabric' && (task.logisticsCompany || task.offlineLogisticsNote)" class="logistics-info">
          <div v-if="task.logisticsCompany">
            物流: {{ task.logisticsCompany }} - {{ task.logisticsTrackingNo || '未填写单号' }}
          </div>
          <div v-if="task.offlineLogisticsNote">
            备注: {{ task.offlineLogisticsNote }}
          </div>
        </div>

        <!-- 拒绝原因 -->
        <div v-if="task.status === 'rejected' && task.rejectReason" class="reject-reason">
          拒绝原因: {{ task.rejectReason }}
        </div>

        <!-- 操作按钮 -->
        <div class="task-actions">
          <template v-if="task.status === 'pending'">
            <button
              @click="acceptTask(task)"
              :disabled="!task.canAccept"
              class="btn btn-primary"
              title="!task.canAccept ? task.cannotAcceptReason : ''"
            >
              接受
            </button>
            <button @click="openRejectDialog(task)" class="btn btn-secondary">
              拒绝
            </button>
          </template>

          <template v-if="task.status === 'accepted'">
            <button
              v-if="task.taskType === 'fabric'"
              @click="openShipDialog(task)"
              class="btn btn-primary"
            >
              发货
            </button>
            <button
              v-if="task.taskType === 'pattern'"
              @click="openResultDialog(task)"
              class="btn btn-primary"
            >
              上传结果
            </button>
            <button @click="viewDetail(task)" class="btn btn-link">
              查看详情
            </button>
          </template>

          <template v-if="task.status === 'shipped'">
            <button
              v-if="task.taskType === 'pattern'"
              @click="openResultDialog(task)"
              class="btn btn-primary"
            >
              上传结果
            </button>
            <button @click="viewDetail(task)" class="btn btn-link">
              查看详情
            </button>
          </template>

          <template v-if="task.status === 'delivered' && task.taskType === 'pattern'">
            <button @click="openResultDialog(task)" class="btn btn-primary">
              上传结果
            </button>
            <button @click="viewDetail(task)" class="btn btn-link">
              查看详情
            </button>
          </template>

          <template v-if="task.status === 'done'">
            <button v-if="task.resultUrl" @click="window.open(task.resultUrl)" class="btn btn-link">
              查看结果
            </button>
            <button @click="viewDetail(task)" class="btn btn-link">
              查看详情
            </button>
          </template>
        </div>
      </div>
    </div>

    <!-- 发货弹窗 -->
    <dialog v-if="shipDialogVisible" class="dialog-overlay" @click.self="shipDialogVisible = false">
      <div class="dialog">
        <div class="dialog-header">
          <h2>填写物流信息</h2>
          <button @click="shipDialogVisible = false" class="close-btn">×</button>
        </div>
        <div class="dialog-body">
          <div class="form-group">
            <label>物流公司</label>
            <input v-model="shipForm.logisticsCompany" type="text" placeholder="如：顺丰、中通等" />
          </div>
          <div class="form-group">
            <label>运单号</label>
            <input v-model="shipForm.logisticsTrackingNo" type="text" placeholder="物流单号" />
          </div>
          <div class="form-group">
            <label>线下备注</label>
            <textarea v-model="shipForm.offlineLogisticsNote" rows="3" placeholder="如：自提、客户指定物流等"></textarea>
          </div>
        </div>
        <div class="dialog-footer">
          <button @click="shipDialogVisible = false" class="btn btn-secondary">取消</button>
          <button @click="submitShip" class="btn btn-primary">提交</button>
        </div>
      </div>
    </dialog>

    <!-- 拒绝原因弹窗 -->
    <dialog v-if="rejectDialogVisible" class="dialog-overlay" @click.self="rejectDialogVisible = false">
      <div class="dialog">
        <div class="dialog-header">
          <h2>拒绝任务</h2>
          <button @click="rejectDialogVisible = false" class="close-btn">×</button>
        </div>
        <div class="dialog-body">
          <div class="form-group">
            <label>拒绝原因 *</label>
            <textarea v-model="rejectReason" rows="4" placeholder="请说明拒绝原因"></textarea>
          </div>
        </div>
        <div class="dialog-footer">
          <button @click="rejectDialogVisible = false" class="btn btn-secondary">取消</button>
          <button @click="submitReject" class="btn btn-danger">确认拒绝</button>
        </div>
      </div>
    </dialog>

    <!-- 上传结果弹窗 -->
    <dialog v-if="resultDialogVisible" class="dialog-overlay" @click.self="resultDialogVisible = false">
      <div class="dialog">
        <div class="dialog-header">
          <h2>上传任务结果</h2>
          <button @click="resultDialogVisible = false" class="close-btn">×</button>
        </div>
        <div class="dialog-body">
          <div class="form-group">
            <label>结果文件 URL *</label>
            <input v-model="resultUrl" type="text" placeholder="文件访问地址" />
          </div>
        </div>
        <div class="dialog-footer">
          <button @click="resultDialogVisible = false" class="btn btn-secondary">取消</button>
          <button @click="submitResult" class="btn btn-primary">提交</button>
        </div>
      </div>
    </dialog>
  </div>
</template>

<style scoped>
.my-tasks-page {
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
  color: #1f2937;
  font-size: 24px;
}

.badge {
  background: #ef4444;
  color: white;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 14px;
  margin-left: 8px;
}

.filters {
  display: flex;
  gap: 12px;
}

.filter-select {
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 14px;
  background: white;
  cursor: pointer;
}

.tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  border-bottom: 1px solid #e5e7eb;
}

.tab {
  padding: 10px 16px;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  color: #6b7280;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.tab.active {
  color: #10a37f;
  border-bottom-color: #10a37f;
}

.tab:hover {
  color: #10a37f;
}

.loading {
  text-align: center;
  padding: 40px;
  color: #6b7280;
}

.empty {
  text-align: center;
  padding: 60px 20px;
  color: #9ca3af;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.task-card {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 20px;
  transition: box-shadow 0.2s;
}

.task-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.task-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.task-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.task-type {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.task-fabric {
  background: #d1fae5;
  color: #059669;
}

.task-pattern {
  background: #fef3c7;
  color: #d97706;
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

.status-pending {
  background: #dbeafe;
  color: #2563eb;
}

.status-accepted {
  background: #d1fae5;
  color: #059669;
}

.status-shipped {
  background: #e0e7ff;
  color: #4f46e5;
}

.status-delivered {
  background: #f3e8ff;
  color: #7c3aed;
}

.status-rejected {
  background: #fee2e2;
  color: #dc2626;
}

.status-done {
  background: #d1fae5;
  color: #059669;
}

.status-cancelled {
  background: #f3f4f6;
  color: #6b7280;
}

.task-content {
  background: #f9fafb;
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 12px;
  overflow-x: auto;
}

.task-content pre {
  margin: 0;
  font-size: 13px;
  color: #374151;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.task-content p {
  margin: 0;
  color: #9ca3af;
  font-size: 14px;
}

.task-meta {
  display: flex;
  gap: 20px;
  margin-bottom: 8px;
  font-size: 13px;
  color: #6b7280;
}

.logistics-info,
.reject-reason {
  padding: 12px;
  background: #fef2f2;
  border-radius: 8px;
  margin-bottom: 12px;
  font-size: 14px;
  color: #991b1b;
}

.logistics-info div {
  margin-bottom: 4px;
}

.logistics-info div:last-child {
  margin-bottom: 0;
}

.task-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.btn {
  padding: 8px 16px;
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
  background: #10a37f;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #0d8a6a;
}

.btn-secondary {
  background: #f3f4f6;
  color: #374151;
}

.btn-secondary:hover:not(:disabled) {
  background: #e5e7eb;
}

.btn-link {
  background: none;
  color: #10a37f;
  padding: 8px 12px;
}

.btn-link:hover {
  text-decoration: underline;
}

.btn-danger {
  background: #fef2f2;
  color: #dc2626;
}

.btn-danger:hover {
  background: #fee2e2;
}

/* Dialog */
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.dialog {
  background: white;
  border-radius: 12px;
  width: 100%;
  max-width: 500px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #e5e7eb;
}

.dialog-header h2 {
  margin: 0;
  font-size: 18px;
  color: #1f2937;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: #9ca3af;
  cursor: pointer;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.dialog-body {
  padding: 20px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group:last-child {
  margin-bottom: 0;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  font-size: 14px;
  font-weight: 500;
  color: #374151;
}

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 14px;
}

.form-group input:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #10a37f;
  box-shadow: 0 0 0 3px rgba(16, 163, 127, 0.1);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 20px;
  border-top: 1px solid #e5e7eb;
}
</style>