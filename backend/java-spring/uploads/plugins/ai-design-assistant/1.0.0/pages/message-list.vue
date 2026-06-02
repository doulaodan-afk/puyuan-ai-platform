<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMessageList, markMessageRead, getUnreadCount } from '../api'
import { useDesignAssistantStore } from '../stores'

const router = useRouter()
const store = useDesignAssistantStore()

// 筛选状态
const filterType = ref<string>('')
const showUnreadOnly = ref(false)
const currentPage = ref(1)
const pageSize = 20

// 加载状态
const loading = ref(false)

// 轮询定时器
let pollTimer: number | null = null

// 消息列表
const messages = computed(() => {
  let filtered = store.messages

  if (filterType.value) {
    filtered = filtered.filter(m => m.type === filterType.value)
  }

  if (showUnreadOnly.value) {
    filtered = filtered.filter(m => !m.isRead)
  }

  return filtered
})

// 未读数量
const unreadCount = computed(() => store.unreadCount)

// 是否显示未读徽章
const hasUnread = computed(() => unreadCount.value > 0)

onMounted(async () => {
  await fetchMessages()
  startPolling()
})

onUnmounted(() => {
  stopPolling()
})

async function fetchMessages() {
  loading.value = true
  try {
    const response = await getMessageList({
      type: filterType.value || undefined,
      unreadOnly: showUnreadOnly.value,
      page: currentPage.value,
      size: pageSize,
    })
    store.setMessages(response.messages, response.total)
  } catch (e: any) {
    console.error('获取消息失败', e)
  } finally {
    loading.value = false
  }
}

async function refreshUnreadCount() {
  try {
    const count = await getUnreadCount()
    store.unreadCount = count
  } catch (e) {
    console.error('获取未读数失败', e)
  }
}

async function markAsRead(message: any) {
  if (message.isRead) return

  try {
    await markMessageRead(message.id)
    store.markMessageAsRead(message.id)
  } catch (e: any) {
    console.error('标记失败', e)
  }
}

async function markAllAsRead() {
  const unread = messages.value.filter(m => !m.isRead)
  for (const msg of unread) {
    await markAsRead(msg)
  }
}

function getMessageTypeIcon(type: string) {
  const icons: Record<string, string> = {
    system: '🔔',
    task: '📋',
    remind: '⏰',
  }
  return icons[type] || '📬'
}

function getMessageTypeLabel(type: string) {
  const labels: Record<string, string> = {
    system: '系统',
    task: '任务',
    remind: '提醒',
  }
  return labels[type] || type
}

function formatTime(time: string) {
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  if (diff < 3600000) {
    return `${Math.floor(diff / 60000)} 分钟前`
  }
  if (diff < 86400000) {
    return `${Math.floor(diff / 3600000)} 小时前`
  }
  if (diff < 604800000) {
    return `${Math.floor(diff / 86400000)} 天前`
  }
  return date.toLocaleDateString('zh-CN')
}

function goToRelated(message: any) {
  if (message.relatedId) {
    if (message.type === 'task') {
      router.push(`/plugins/ai-design-assistant/tasks`)
    }
  }
}

function startPolling() {
  refreshUnreadCount()
  pollTimer = window.setInterval(refreshUnreadCount, 30000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}
</script>

<template>
  <div class="message-center-page page-container">
    <header class="header">
      <h1>
        消息中心
        <span v-if="hasUnread" class="unread-badge">{{ unreadCount }}</span>
      </h1>
      <div class="header-actions">
        <button @click="markAllAsRead" :disabled="!hasUnread" class="btn btn-secondary">
          全部已读
        </button>
        <button @click="fetchMessages" class="btn btn-secondary">
          刷新
        </button>
      </div>
    </header>

    <div class="filters">
      <button
        :class="['filter-btn', { active: filterType === '' }]"
        @click="filterType = ''; fetchMessages()"
      >
        全部
      </button>
      <button
        :class="['filter-btn', { active: filterType === 'system' }]"
        @click="filterType = 'system'; fetchMessages()"
      >
        系统
      </button>
      <button
        :class="['filter-btn', { active: filterType === 'task' }]"
        @click="filterType = 'task'; fetchMessages()"
      >
        任务
      </button>
      <button
        :class="['filter-btn', { active: filterType === 'remind' }]"
        @click="filterType = 'remind'; fetchMessages()"
      >
        提醒
      </button>
      <label class="checkbox-label">
        <input v-model="showUnreadOnly" type="checkbox" @change="fetchMessages" />
        仅显示未读
      </label>
    </div>

    <div v-if="loading" class="loading">加载中...</div>

    <div v-else-if="messages.length === 0" class="empty">
      <div class="empty-icon">📬</div>
      <p>暂无消息</p>
    </div>

    <div v-else class="message-list">
      <div
        v-for="message in messages"
        :key="message.id"
        :class="['message-item', { unread: !message.isRead }]"
        @click="markAsRead(message)"
      >
        <div class="message-icon">
          {{ getMessageTypeIcon(message.type) }}
        </div>
        <div class="message-main">
          <div class="message-header">
            <div class="message-title">
              <span class="type-badge">{{ getMessageTypeLabel(message.type) }}</span>
              <span class="title-text">{{ message.title }}</span>
            </div>
            <div class="message-time">{{ formatTime(message.createdAt) }}</div>
          </div>
          <div class="message-content">{{ message.content }}</div>
        </div>
        <div v-if="!message.isRead" class="unread-dot"></div>
        <button
          v-if="message.relatedId"
          @click.stop="goToRelated(message)"
          class="view-related-btn"
          title="查看关联内容"
        >
          →
        </button>
      </div>
    </div>

    <div v-if="store.messagesTotal > pageSize" class="pagination">
      <button @click="currentPage--" :disabled="currentPage <= 1" class="pagination-btn">
        上一页
      </button>
      <span class="pagination-info">
        第 {{ currentPage }} 页，共 {{ Math.ceil(store.messagesTotal / pageSize) }} 页
      </span>
      <button
        @click="currentPage++"
        :disabled="currentPage >= Math.ceil(store.messagesTotal / pageSize)"
        class="pagination-btn"
      >
        下一页
      </button>
    </div>
  </div>
</template>

<style scoped>
.message-center-page {
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
  color: #1f2937;
  font-size: 24px;
}

.unread-badge {
  background: #ef4444;
  color: white;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.filters {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  padding: 12px;
  background: white;
  border-radius: 8px;
  flex-wrap: wrap;
}

.filter-btn {
  padding: 6px 16px;
  background: #f3f4f6;
  border: none;
  border-radius: 16px;
  font-size: 14px;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s;
}

.filter-btn:hover {
  background: #e5e7eb;
}

.filter-btn.active {
  background: #10a37f;
  color: white;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #374151;
  margin-left: 12px;
}

.loading {
  text-align: center;
  padding: 40px;
  color: #6b7280;
}

.empty {
  text-align: center;
  padding: 80px 20px;
  color: #9ca3af;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.message-item {
  position: relative;
  display: flex;
  gap: 16px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s;
}

.message-item:hover {
  border-color: #d1d5db;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.message-item.unread {
  background: #f0fdf4;
  border-color: #86efac;
}

.message-icon {
  width: 40px;
  height: 40px;
  background: #f3f4f6;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.message-item.unread .message-icon {
  background: #10a37f;
}

.message-main {
  flex: 1;
  min-width: 0;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.message-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.type-badge {
  padding: 2px 8px;
  background: #f3f4f6;
  border-radius: 4px;
  font-size: 12px;
  color: #6b7280;
}

.message-item.unread .type-badge {
  background: rgba(16, 163, 127, 0.1);
  color: #059669;
}

.title-text {
  font-size: 15px;
  font-weight: 500;
  color: #1f2937;
}

.message-time {
  font-size: 12px;
  color: #9ca3af;
}

.message-content {
  font-size: 14px;
  color: #4b5563;
  line-height: 1.5;
}

.unread-dot {
  position: absolute;
  top: 16px;
  right: 50px;
  width: 8px;
  height: 8px;
  background: #ef4444;
  border-radius: 50%;
}

.view-related-btn {
  position: absolute;
  top: 50%;
  right: 16px;
  transform: translateY(-50%);
  background: #10a37f;
  color: white;
  border: none;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  cursor: pointer;
  font-size: 16px;
  opacity: 0;
  transition: opacity 0.2s;
}

.message-item:hover .view-related-btn {
  opacity: 1;
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

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 24px;
  padding: 16px;
}

.pagination-btn {
  padding: 8px 16px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}

.pagination-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.pagination-info {
  font-size: 14px;
  color: #6b7280;
}
</style>