import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type {
  ChatMessage,
  RequirementListItem,
  RequirementDetail,
  TaskInfo,
  FabricLibraryItem,
  MessageInfo,
  TaskAssignRule,
  AiSummary,
} from '../types'

// 身份信息接口
export interface IdentityInfo {
  tenantId: number
  tenantName: string
  role: string
  roleLabel: string
  identityPrefix: string
}

export const useDesignAssistantStore = defineStore('designAssistant', () => {
  // ========== 身份相关 ==========
  const identity = ref<IdentityInfo | null>(null)

  // 从 localStorage 恢复身份
  function restoreIdentity() {
    const prefix = localStorage.getItem('ai_design_identity_prefix')
    const tenantId = localStorage.getItem('ai_design_tenant_id')
    const role = localStorage.getItem('ai_design_role')

    if (prefix && tenantId && role) {
      // 从前缀中解析工作室名称和角色标签
      const lastDash = prefix.lastIndexOf('-')
      const tenantName = lastDash > 0 ? prefix.substring(0, lastDash) : prefix
      const roleLabel = lastDash > 0 ? prefix.substring(lastDash + 1) : ''

      identity.value = {
        tenantId: parseInt(tenantId),
        tenantName,
        role,
        roleLabel,
        identityPrefix: prefix,
      }
    }
  }

  // 设置身份
  function setIdentity(info: IdentityInfo) {
    identity.value = info
    localStorage.setItem('ai_design_identity_prefix', info.identityPrefix)
    localStorage.setItem('ai_design_tenant_id', String(info.tenantId))
    localStorage.setItem('ai_design_role', info.role)
  }

  // 清除身份
  function clearIdentity() {
    identity.value = null
    localStorage.removeItem('ai_design_identity_prefix')
    localStorage.removeItem('ai_design_tenant_id')
    localStorage.removeItem('ai_design_role')
  }

  // 是否已选择身份
  const hasIdentity = computed(() => identity.value !== null)

  // 身份显示名称
  const identityDisplayName = computed(() => {
    if (!identity.value) return ''
    return `${identity.value.tenantName} - ${identity.value.roleLabel}`
  })

  // ========== 对话相关 ==========
  const currentSessionId = ref<string>('')
  const conversationHistory = ref<ChatMessage[]>([])
  const currentRequirementId = ref<number | null>(null)

  // AI 总结
  const aiSummary = ref<AiSummary | null>(null)
  const summaryText = ref<string>('')

  // 上传的文件
  const uploadedImages = ref<string[]>([])
  const uploadedVideos = ref<string[]>([])

  // 加载状态
  const isChatting = ref(false)
  const isSummarizing = ref(false)

  // ========== 需求列表 ==========
  const requirements = ref<RequirementListItem[]>([])
  const requirementDetail = ref<RequirementDetail | null>(null)

  // ========== 任务相关 ==========
  const myTasks = ref<TaskInfo[]>([])
  const myTasksTotal = ref(0)
  const myTasksPage = ref(1)
  const myTasksSize = ref(20)

  // ========== 助理相关 ==========
  const pendingRequirements = ref<RequirementListItem[]>([])
  const currentEditingTasks = ref<TaskInfo[]>([])

  // ========== 消息相关 ==========
  const messages = ref<MessageInfo[]>([])
  const messagesTotal = ref(0)
  const unreadCount = ref(0)

  // ========== 面料库相关 ==========
  const fabricLibrary = ref<FabricLibraryItem[]>([])
  const myFabrics = ref<FabricLibraryItem[]>([])
  const fabricLibraryTotal = ref(0)

  // ========== 分配规则（管理端） ==========
  const assignRules = ref<TaskAssignRule[]>([])

  // ========== 统计数据 ==========
  const statistics = ref({
    totalRequirements: 0,
    draftCount: 0,
    processingCount: 0,
    releasedCount: 0,
    completedCount: 0,
    totalTokens: 0,
    totalTasks: 0,
    completedTasks: 0,
    taskCompletionRate: 0,
  })

  // ========== 计算属性 ==========

  // 对话相关
  const hasConversation = computed(() => conversationHistory.value.length > 0)
  const hasAiSummary = computed(() => aiSummary.value !== null)

  // 任务统计
  const pendingTasksCount = computed(() =>
    myTasks.value.filter(t => t.status === 'pending').length
  )
  const acceptedTasksCount = computed(() =>
    myTasks.value.filter(t => t.status === 'accepted').length
  )
  const completedTasksCount = computed(() =>
    myTasks.value.filter(t => t.status === 'done' || t.status === 'delivered').length
  )

  // 消息统计
  const hasUnreadMessages = computed(() => unreadCount.value > 0)

  // ========== 动作 ==========

  // 对话相关
  function setSessionId(sessionId: string) {
    currentSessionId.value = sessionId
  }

  function addMessage(message: ChatMessage) {
    conversationHistory.value.push(message)
  }

  function setConversation(history: ChatMessage[]) {
    conversationHistory.value = history
  }

  function clearConversation() {
    conversationHistory.value = []
    currentSessionId.value = ''
    aiSummary.value = null
    summaryText.value = ''
    uploadedImages.value = []
    uploadedVideos.value = []
  }

  function setAiSummary(summary: AiSummary | null, text?: string) {
    aiSummary.value = summary
    if (text) summaryText.value = text
  }

  function addImage(url: string) {
    uploadedImages.value.push(url)
  }

  function removeImage(index: number) {
    uploadedImages.value.splice(index, 1)
  }

  function addVideo(url: string) {
    uploadedVideos.value.push(url)
  }

  function removeVideo(index: number) {
    uploadedVideos.value.splice(index, 1)
  }

  // 需求相关
  function setRequirements(list: RequirementListItem[]) {
    requirements.value = list
  }

  function setRequirementDetail(detail: RequirementDetail) {
    requirementDetail.value = detail
  }

  // 任务相关
  function setMyTasks(tasks: TaskInfo[], total: number) {
    myTasks.value = tasks
    myTasksTotal.value = total
  }

  function updateTaskStatus(taskId: number, status: string) {
    const task = myTasks.value.find(t => t.id === taskId)
    if (task) {
      task.status = status as any
    }
  }

  // 助理相关
  function setPendingRequirements(list: RequirementListItem[]) {
    pendingRequirements.value = list
  }

  function setEditingTasks(tasks: TaskInfo[]) {
    currentEditingTasks.value = tasks
  }

  // 消息相关
  function setMessages(messageList: MessageInfo[], total: number) {
    messages.value = messageList
    messagesTotal.value = total
    unreadCount.value = messageList.filter(m => !m.isRead).length
  }

  function markMessageAsRead(messageId: number) {
    const msg = messages.value.find(m => m.id === messageId)
    if (msg) {
      msg.isRead = true
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    }
  }

  function addMessageToInbox(msg: MessageInfo) {
    messages.value.unshift(msg)
    messagesTotal.value += 1
    if (!msg.isRead) {
      unreadCount.value += 1
    }
  }

  // 面料库相关
  function setFabricLibrary(list: FabricLibraryItem[], total: number) {
    fabricLibrary.value = list
    fabricLibraryTotal.value = total
  }

  function setMyFabrics(list: FabricLibraryItem[]) {
    myFabrics.value = list
  }

  function addFabric(fabric: FabricLibraryItem) {
    myFabrics.value.unshift(fabric)
    fabricLibrary.value.unshift(fabric)
  }

  function updateFabric(fabric: FabricLibraryItem) {
    const idx = myFabrics.value.findIndex(f => f.id === fabric.id)
    if (idx !== -1) {
      myFabrics.value[idx] = fabric
    }
    const idx2 = fabricLibrary.value.findIndex(f => f.id === fabric.id)
    if (idx2 !== -1) {
      fabricLibrary.value[idx2] = fabric
    }
  }

  function removeFabric(fabricId: number) {
    myFabrics.value = myFabrics.value.filter(f => f.id !== fabricId)
    fabricLibrary.value = fabricLibrary.value.filter(f => f.id !== fabricId)
  }

  // 分配规则相关
  function setAssignRules(rules: TaskAssignRule[]) {
    assignRules.value = rules
  }

  // 统计数据
  function setStatistics(stats: typeof statistics.value) {
    statistics.value = stats
  }

  // 重置（保留身份）
  function reset() {
    clearConversation()
    currentRequirementId.value = null
    requirementDetail.value = null
  }

  function resetAll() {
    reset()
    requirements.value = []
    myTasks.value = []
    myTasksTotal.value = 0
    pendingRequirements.value = []
    currentEditingTasks.value = []
    messages.value = []
    messagesTotal.value = 0
    unreadCount.value = 0
    fabricLibrary.value = []
    myFabrics.value = []
    fabricLibraryTotal.value = 0
    assignRules.value = []
  }

  // 初始化时尝试恢复身份
  restoreIdentity()

  return {
    // 身份状态
    identity,
    hasIdentity,
    identityDisplayName,
    setIdentity,
    clearIdentity,
    restoreIdentity,

    // 状态
    currentSessionId,
    conversationHistory,
    currentRequirementId,
    aiSummary,
    summaryText,
    uploadedImages,
    uploadedVideos,
    isChatting,
    isSummarizing,
    requirements,
    requirementDetail,
    myTasks,
    myTasksTotal,
    myTasksPage,
    myTasksSize,
    pendingRequirements,
    currentEditingTasks,
    messages,
    messagesTotal,
    unreadCount,
    fabricLibrary,
    myFabrics,
    fabricLibraryTotal,
    assignRules,
    statistics,

    // 计算属性
    hasConversation,
    hasAiSummary,
    pendingTasksCount,
    acceptedTasksCount,
    completedTasksCount,
    hasUnreadMessages,

    // 动作
    setSessionId,
    addMessage,
    setConversation,
    clearConversation,
    setAiSummary,
    addImage,
    removeImage,
    addVideo,
    removeVideo,
    setRequirements,
    setRequirementDetail,
    setMyTasks,
    updateTaskStatus,
    setPendingRequirements,
    setEditingTasks,
    setMessages,
    markMessageAsRead,
    addMessageToInbox,
    setFabricLibrary,
    setMyFabrics,
    addFabric,
    updateFabric,
    removeFabric,
    setAssignRules,
    setStatistics,
    reset,
    resetAll,
  }
})
