<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getRequirementDetail,
  editTask,
  createTask,
  deleteTask,
  publishRequirement,
} from '../api/design-assistant'
import { useDesignAssistantStore } from '../stores/designAssistant'
import type { TaskInfo, RequirementDetail } from '../types/design-assistant'

const route = useRoute()
const router = useRouter()
const store = useDesignAssistantStore()

const requirementId = ref<number>(Number(route.params.id))
const requirement = ref<RequirementDetail | null>(null)
const tasks = ref<TaskInfo[]>([])

// 加载状态
const loading = ref(false)
const publishing = ref(false)

// 编辑弹窗
const editDialogVisible = ref(false)
const editingTask = ref<TaskInfo | null>(null)
const editForm = ref({
  assigneeId: 0,
  deadline: '',
  content: '',
})

// 新增任务弹窗
const createDialogVisible = ref(false)
const createForm = ref({
  taskType: 'fabric',
  assigneeType: 'supplier',
  assigneeId: 0,
  content: '{}',
  deadline: '',
  fabricTaskId: 0,
})

// 模拟选项
const assigneeOptions = ref([
  { id: 3001, name: '真丝面料供应商' },
  { id: 3002, name: '羊毛面料供应商' },
  { id: 3003, name: '棉麻面料供应商' },
  { id: 4001, name: '通用版师服务商' },
])

// 草稿任务
const draftTasks = computed(() => tasks.value.filter(t => t.status === 'draft'))

// 已发布任务
const publishedTasks = computed(() => tasks.value.filter(t => t.status !== 'draft'))

onMounted(async () => {
  await fetchDetail()
})

async function fetchDetail() {
  loading.value = true
  try {
    requirement.value = await getRequirementDetail(requirementId.value)
    tasks.value = requirement.value.tasks || []
  } catch (e: any) {
    console.error('获取详情失败', e)
    alert('加载失败: ' + e.message)
  } finally {
    loading.value = false
  }
}

function getTaskTypeLabel(type: string) {
  return type === 'fabric' ? '面料' : '打版'
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

function openEditDialog(task: TaskInfo) {
  editingTask.value = task
  editForm.value = {
    assigneeId: task.assigneeId,
    deadline: task.deadline || '',
    content: JSON.stringify(task.content, null, 2),
  }
  editDialogVisible.value = true
}

async function saveEdit() {
  if (!editingTask.value) return

  try {
    const content = JSON.parse(editForm.value.content)
    await editTask({
      taskId: editingTask.value.id,
      assigneeId: editForm.value.assigneeId || undefined,
      deadline: editForm.value.deadline || undefined,
      content: Object.keys(content).length > 0 ? content : undefined,
    })
    alert('更新成功')
    editDialogVisible.value = false
    await fetchDetail()
  } catch (e: any) {
    alert('保存失败: ' + e.message)
  }
}

function openCreateDialog() {
  createForm.value = {
    taskType: 'fabric',
    assigneeType: 'supplier',
    assigneeId: 0,
    content: '{}',
    deadline: new Date(Date.now() + 7 * 24 * 3600 * 1000).toISOString().slice(0, 16),
    fabricTaskId: 0,
  }
  createDialogVisible.value = true
}

async function saveCreate() {
  if (!requirement.value) return

  try {
    const content = JSON.parse(createForm.value.content)
    await createTask({
      requirementId: requirement.value.id,
      taskType: createForm.value.taskType,
      assigneeType: createForm.value.assigneeType,
      assigneeId: createForm.value.assigneeId,
      content,
      deadline: createForm.value.deadline || undefined,
      fabricTaskId: createForm.value.fabricTaskId || undefined,
    })
    alert('添加成功')
    createDialogVisible.value = false
    await fetchDetail()
  } catch (e: any) {
    alert('创建失败: ' + e.message)
  }
}

async function removeTask(taskId: number) {
  if (!confirm('确认删除此任务？')) return

  try {
    await deleteTask(taskId)
    alert('删除成功')
    await fetchDetail()
  } catch (e: any) {
    alert('删除失败: ' + e.message)
  }
}

async function publish() {
  if (!requirement.value) return
  if (draftTasks.value.length === 0) {
    alert('没有草稿任务需要发布')
    return
  }

  if (!confirm(`确认发布 ${draftTasks.value.length} 个草稿任务？发布后将推送给面料商和版师。`)) {
    return
  }

  publishing.value = true
  try {
    await publishRequirement(requirement.value.id, false)
    alert('发布成功')
    await fetchDetail()
  } catch (e: any) {
    alert('发布失败: ' + e.message)
  } finally {
    publishing.value = false
  }
}

function goBack() {
  router.back()
}

function formatDate(dateStr: string | null) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

// 获取分配对象名称
function getAssigneeName(id: number) {
  const option = assigneeOptions.value.find(o => o.id === id)
  return option?.name || `ID: ${id}`
}

// 查看关联面料任务
function getFabricTaskInfo(task: TaskInfo) {
  if (task.taskType !== 'pattern' || task.fabricTaskId === 0) return null
  return tasks.value.find(t => t.id === task.fabricTaskId)
}
</script>

<template>
  <div class="assistant-detail-page page-container">
    <header class="header">
      <button @click="goBack" class="back-btn">← 返回</button>
      <h1>需求复核与任务编辑</h1>
    </header>

    <div v-if="loading" class="loading">加载中...</div>

    <div v-else-if="requirement" class="content">
      <!-- 需求信息 -->
      <div class="section">
        <h2>需求信息</h2>
        <div class="info-card">
          <div class="info-row">
            <span class="label">标题:</span>
            <span class="value">{{ requirement.title }}</span>
          </div>
          <div class="info-row">
            <span class="label">原始描述:</span>
            <span class="value">{{ requirement.rawText }}</span>
          </div>
          <div v-if="requirement.rawImages?.length > 0" class="info-row">
            <span class="label">参考图片:</span>
            <div class="image-previews">
              <img
                v-for="(img, i) in requirement.rawImages"
                :key="i"
                :src="img"
                alt=""
              />
            </div>
          </div>
          <div v-if="requirement.aiSummary" class="info-row full-width">
            <span class="label">AI 总结:</span>
            <pre class="summary-text">{{ requirement.aiSummary }}</pre>
          </div>
          <div class="info-row">
            <span class="label">创建时间:</span>
            <span class="value">{{ formatDate(requirement.createdAt) }}</span>
          </div>
          <div class="info-row">
            <span class="label">消耗 Token:</span>
            <span class="value">{{ requirement.totalTokenCost }}</span>
          </div>
        </div>
      </div>

      <!-- 草稿任务（可编辑） -->
      <div class="section">
        <div class="section-header">
          <h2>草稿任务 (可编辑)</h2>
          <button @click="openCreateDialog" class="btn btn-secondary">
            + 新增任务
          </button>
        </div>

        <div v-if="draftTasks.length === 0" class="empty">
          <p>暂无草稿任务</p>
        </div>

        <div v-else class="task-list">
          <div v-for="task in draftTasks" :key="task.id" class="task-card draft">
            <div class="task-header">
              <span :class="['task-type', task.taskType === 'fabric' ? 'fabric' : 'pattern']">
                {{ getTaskTypeLabel(task.taskType) }}
              </span>
              <span class="task-id">#{{ task.id }}</span>
            </div>

            <div class="task-content">
              <pre>{{ JSON.stringify(task.content, null, 2) }}</pre>
            </div>

            <div class="task-meta">
              <div class="meta-item">
                <label>分配对象:</label>
                <span>{{ getAssigneeName(task.assigneeId) }}</span>
              </div>
              <div class="meta-item">
                <label>截止时间:</label>
                <span>{{ formatDate(task.deadline) }}</span>
              </div>
              <!-- Pattern 任务的关联面料 -->
              <div v-if="task.taskType === 'pattern' && task.fabricTaskId > 0" class="meta-item">
                <label>关联面料任务:</label>
                <span>#{{ task.fabricTaskId }}</span>
              </div>
            </div>

            <div class="task-actions">
              <button @click="openEditDialog(task)" class="btn btn-secondary">
                编辑
              </button>
              <button @click="removeTask(task.id)" class="btn btn-danger">
                删除
              </button>
            </div>
          </div>
        </div>

        <div v-if="draftTasks.length > 0" class="publish-section">
          <button
            @click="publish"
            :disabled="publishing"
            class="btn btn-primary btn-large"
          >
            {{ publishing ? '发布中...' : `发布 ${draftTasks.length} 个任务` }}
          </button>
        </div>
      </div>

      <!-- 已发布任务（只读） -->
      <div v-if="publishedTasks.length > 0" class="section">
        <h2>已发布任务</h2>
        <div class="task-list">
          <div v-for="task in publishedTasks" :key="task.id" class="task-card">
            <div class="task-header">
              <span :class="['task-type', task.taskType === 'fabric' ? 'fabric' : 'pattern']">
                {{ getTaskTypeLabel(task.taskType) }}
              </span>
              <span :class="['status-badge', 'status-' + task.status]">
                {{ getStatusLabel(task.status) }}
              </span>
            </div>

            <div class="task-content">
              <pre>{{ JSON.stringify(task.content, null, 2) }}</pre>
            </div>

            <div class="task-meta">
              <div class="meta-item">
                <label>分配对象:</label>
                <span>{{ getAssigneeName(task.assigneeId) }}</span>
              </div>
              <div class="meta-item">
                <label>截止时间:</label>
                <span>{{ formatDate(task.deadline) }}</span>
              </div>
              <!-- 物流信息（仅面料） -->
              <div v-if="task.taskType === 'fabric' && task.logisticsCompany" class="meta-item">
                <label>物流:</label>
                <span>{{ task.logisticsCompany }} - {{ task.logisticsTrackingNo }}</span>
              </div>
              <!-- 结果文件 -->
              <div v-if="task.resultUrl" class="meta-item">
                <label>结果:</label>
                <a :href="task.resultUrl" target="_blank" class="result-link">
                  查看文件
                </a>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 编辑任务弹窗 -->
    <dialog v-if="editDialogVisible" class="dialog-overlay" @click.self="editDialogVisible = false">
      <div class="dialog">
        <div class="dialog-header">
          <h2>编辑任务</h2>
          <button @click="editDialogVisible = false" class="close-btn">×</button>
        </div>
        <div class="dialog-body">
          <div class="form-group">
            <label>分配对象</label>
            <select v-model="editForm.assigneeId" class="input">
              <option :value="0">请选择</option>
              <option v-for="opt in assigneeOptions" :key="opt.id" :value="opt.id">
                {{ opt.name }}
              </option>
            </select>
          </div>
          <div class="form-group">
            <label>截止时间</label>
            <input
              v-model="editForm.deadline"
              type="datetime-local"
              class="input"
            />
          </div>
          <div class="form-group">
            <label>任务内容 (JSON)</label>
            <textarea
              v-model="editForm.content"
              rows="8"
              class="input textarea"
            ></textarea>
          </div>
        </div>
        <div class="dialog-footer">
          <button @click="editDialogVisible = false" class="btn btn-secondary">取消</button>
          <button @click="saveEdit" class="btn btn-primary">保存</button>
        </div>
      </div>
    </dialog>

    <!-- 新增任务弹窗 -->
    <dialog v-if="createDialogVisible" class="dialog-overlay" @click.self="createDialogVisible = false">
      <div class="dialog">
        <div class="dialog-header">
          <h2>新增任务</h2>
          <button @click="createDialogVisible = false" class="close-btn">×</button>
        </div>
        <div class="dialog-body">
          <div class="form-row">
            <div class="form-group">
              <label>任务类型</label>
              <select v-model="createForm.taskType" class="input">
                <option value="fabric">面料任务</option>
                <option value="pattern">打版任务</option>
              </select>
            </div>
            <div class="form-group">
              <label>分配类型</label>
              <select
                v-model="createForm.assigneeType"
                :disabled="createForm.taskType === 'pattern'"
                class="input"
              >
                <option value="supplier">面料商</option>
                <option value="pattern_service">版师服务商</option>
              </select>
            </div>
          </div>
          <div class="form-group">
            <label>分配对象</label>
            <select v-model="createForm.assigneeId" class="input">
              <option :value="0">请选择</option>
              <option v-for="opt in assigneeOptions" :key="opt.id" :value="opt.id">
                {{ opt.name }}
              </option>
            </select>
          </div>
          <div class="form-group">
            <label>截止时间</label>
            <input
              v-model="createForm.deadline"
              type="datetime-local"
              class="input"
            />
          </div>
          <div class="form-group">
            <label>任务内容 (JSON)</label>
            <textarea
              v-model="createForm.content"
              rows="8"
              class="input textarea"
            ></textarea>
          </div>
        </div>
        <div class="dialog-footer">
          <button @click="createDialogVisible = false" class="btn btn-secondary">取消</button>
          <button @click="saveCreate" class="btn btn-primary">创建</button>
        </div>
      </div>
    </dialog>
  </div>
</template>

<style scoped>
.assistant-detail-page {
  padding: 20px;
}

.header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.back-btn {
  background: none;
  border: none;
  font-size: 20px;
  color: #6b7280;
  cursor: pointer;
  padding: 4px 8px;
}

.back-btn:hover {
  color: #374151;
}

.header h1 {
  margin: 0;
  color: #1f2937;
  font-size: 22px;
}

.loading {
  text-align: center;
  padding: 40px;
  color: #6b7280;
}

.section {
  margin-bottom: 32px;
}

.section h2 {
  margin: 0 0 16px;
  color: #1f2937;
  font-size: 18px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.info-card {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 20px;
}

.info-row {
  display: flex;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid #f3f4f6;
}

.info-row:last-child {
  border-bottom: none;
}

.info-row.full-width {
  flex-direction: column;
}

.info-row .label {
  width: 100px;
  font-weight: 500;
  color: #6b7280;
  font-size: 14px;
}

.info-row .value {
  flex: 1;
  color: #374151;
  font-size: 14px;
}

.image-previews {
  display: flex;
  gap: 12px;
}

.image-previews img {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.summary-text {
  margin: 8px 0 0;
  padding: 12px;
  background: #f9fafb;
  border-radius: 8px;
  font-size: 13px;
  color: #374151;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.empty {
  text-align: center;
  padding: 40px;
  color: #9ca3af;
}

.task-list {
  display: grid;
  gap: 16px;
}

.task-card {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 20px;
}

.task-card.draft {
  border-left: 4px solid #f59e0b;
}

.task-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.task-type {
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
}

.task-type.fabric {
  background: #d1fae5;
  color: #059669;
}

.task-type.pattern {
  background: #fef3c7;
  color: #d97706;
}

.task-id {
  font-size: 12px;
  color: #9ca3af;
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

.status-done {
  background: #d1fae5;
  color: #059669;
}

.task-content {
  background: #f9fafb;
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 12px;
}

.task-content pre {
  margin: 0;
  font-size: 13px;
  color: #374151;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.task-meta {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
  margin-bottom: 12px;
  font-size: 13px;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.meta-item label {
  color: #9ca3af;
  font-size: 12px;
}

.meta-item span {
  color: #374151;
}

.result-link {
  color: #10a37f;
  text-decoration: none;
}

.result-link:hover {
  text-decoration: underline;
}

.task-actions {
  display: flex;
  gap: 8px;
}

.publish-section {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e5e7eb;
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

.btn-danger {
  background: #fef2f2;
  color: #dc2626;
}

.btn-danger:hover {
  background: #fee2e2;
}

.btn-large {
  padding: 12px 32px;
  font-size: 16px;
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
  max-width: 600px;
  max-height: 90vh;
  overflow-y: auto;
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

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  font-size: 14px;
  font-weight: 500;
  color: #374151;
}

.input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 14px;
}

.input:focus {
  outline: none;
  border-color: #10a37f;
  box-shadow: 0 0 0 3px rgba(16, 163, 127, 0.1);
}

.input.textarea {
  font-family: 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', Consolas, monospace;
  resize: vertical;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 20px;
  border-top: 1px solid #e5e7eb;
}
</style>