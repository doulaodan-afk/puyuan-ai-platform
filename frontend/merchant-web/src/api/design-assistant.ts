// AI 设计助手 API 客户端

import { useAuthStore } from '../stores/auth'

const API_BASE = 'http://localhost:8080/api/design'

// 获取请求头：从 auth store 取 tenantId 和 accessToken，解析 userId
function getHeaders() {
  const auth = useAuthStore()
  const tenantId = auth.tenantId || '1'
  let userId = '1'
  if (auth.accessToken) {
    const raw = auth.accessToken.replace('Bearer ', '')
    const parts = raw.split('-')
    if (parts.length >= 2) {
      userId = parts[1]
    }
  }
  return {
    'X-Tenant-Id': tenantId,
    'X-User-Id': userId,
    'Authorization': auth.accessToken ? `Bearer ${auth.accessToken}` : '',
    'Content-Type': 'application/json',
  }
}

// 通用请求函数
async function request<T>(url: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(`${API_BASE}${url}`, {
    ...options,
    headers: { ...getHeaders(), ...options.headers },
  })

  const result = await response.json()

  if (result.code !== 0) {
    throw new Error(result.message || '请求失败')
  }

  return result.data as T
}

// ====== 类型定义 ======

export interface ChatMessage {
  role: 'user' | 'assistant' | 'system'
  content: string
  time: string
}

export interface RequirementListItem {
  id: number
  title: string
  status: string
  totalTokenCost: number
  createdAt: string
  taskCount: number
}

export interface RequirementDetail {
  id: number
  tenantId: number
  creatorId: number
  title: string
  rawImages: string[]
  rawVideos: string[]
  rawAudioUrl: string
  rawText: string
  conversationHistory: ChatMessage[]
  aiSummary: string | null
  designerApproved: number
  assistantId: number
  status: string
  totalTokenCost: number
  createdAt: string
  updatedAt: string
  tasks: TaskInfo[]
}

export interface TaskInfo {
  id: number
  requirementId: number
  taskType: 'fabric' | 'pattern'
  assigneeType: string
  assigneeId: number
  content: Record<string, any>
  status: string
  deadline: string | null
  resultUrl: string | null
  fabricTaskId: number
  logisticsCompany: string | null
  logisticsTrackingNo: string | null
  logisticsStatus: string
  offlineLogisticsNote: string | null
  shippedAt: string | null
  deliveredAt: string | null
  rejectReason: string | null
  completedAt: string | null
  createdAt: string
  updatedAt: string
  requirementTitle: string
  canAccept: boolean
  cannotAcceptReason: string | null
}

export interface FabricInfo {
  id: number
  supplierTenantId: number
  name: string
  category: string | null
  images: string[]
  videoUrl: string | null
  specs: Record<string, any>
  pricePerMeter: number | null
  stockStatus: string
  isVisible: number
  createdAt: string
  updatedAt: string
}

export interface MessageInfo {
  id: number
  receiverId: number
  senderId: number
  title: string
  content: string
  type: string
  isRead: boolean
  relatedId: number
  createdAt: string
}

// ====== 需求管理 ======

export async function createRequirement(data: {
  title: string
  rawImages?: string[]
  rawVideos?: string[]
  rawAudioUrl?: string
  rawText: string
  conversationHistory?: ChatMessage[]
  selectedSupplierId?: number
}) {
  return request<RequirementDetail>('/requirement/create', {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

export async function chat(data: {
  sessionId?: string
  message: string
  requirementId?: number
}) {
  return request<{
    sessionId: string
    assistantMessage: string
    conversationHistory: ChatMessage[]
  }>('/requirement/chat', {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

export async function summarize(requirementId: number) {
  return request<{
    aiSummary: string
    summaryData: Record<string, any>
  }>('/requirement/summarize', {
    method: 'POST',
    body: JSON.stringify({ requirementId }),
  })
}

export async function confirmRequirement(requirementId: number) {
  return request<{ success: boolean; message: string }>(
    '/requirement/confirm',
    {
      method: 'POST',
      body: JSON.stringify({ requirementId }),
    },
  )
}

export async function transferToAssistant(data: {
  requirementId: number
  assistantId?: number
}) {
  return request<{ success: boolean; message: string }>(
    '/requirement/transfer',
    {
      method: 'POST',
      body: JSON.stringify(data),
    },
  )
}

export async function getRequirementList(params: {
  status?: string
  page?: number
  size?: number
}) {
  const query = new URLSearchParams(
    params as any,
  ).toString()
  return request<RequirementListItem[]>(
    `/requirement/list?${query}`,
  )
}

export async function getRequirementDetail(id: number) {
  return request<RequirementDetail>(`/requirement/detail/${id}`)
}

// ====== 任务管理 ======

export async function editTask(data: {
  taskId: number
  assigneeId?: number
  deadline?: string
  content?: Record<string, any>
}) {
  return request<{ success: boolean; message: string }>(
    '/assistant/task',
    {
      method: 'PUT',
      body: JSON.stringify(data),
    },
  )
}

export async function createTask(data: {
  requirementId: number
  taskType: 'fabric' | 'pattern'
  assigneeType: string
  assigneeId: number
  content?: Record<string, any>
  deadline?: string
  fabricTaskId?: number
}) {
  return request<{ success: boolean; message: string }>(
    '/assistant/task',
    {
      method: 'POST',
      body: JSON.stringify(data),
    },
  )
}

export async function deleteTask(taskId: number) {
  return request<{ success: boolean; message: string }>(
    `/assistant/task/${taskId}`,
    {
      method: 'DELETE',
    },
  )
}

export async function publishRequirement(
  requirementId: number,
  forcePublish = false,
) {
  return request<{ success: boolean; message: string }>(
    `/assistant/publish/${requirementId}?forcePublish=${forcePublish}`,
    {
      method: 'POST',
    },
  )
}

export async function getMyTasks(params: {
  status?: string
  taskType?: string
  page?: number
  size?: number
}) {
  const query = new URLSearchParams(
    params as any,
  ).toString()
  return request<{
    tasks: TaskInfo[]
    total: number
    page: number
    size: number
  }>(`/task/my-tasks?${query}`)
}

export async function getTaskDetail(taskId: number) {
  return request<TaskInfo>(`/task/detail/${taskId}`)
}

export async function updateTaskStatus(data: {
  taskId: number
  status: string
  rejectReason?: string
}) {
  return request<{ success: boolean; message: string }>(
    `/task/${data.taskId}/status`,
    {
      method: 'PUT',
      body: JSON.stringify(data),
    },
  )
}

export async function shipTask(data: {
  taskId: number
  logisticsCompany?: string
  logisticsTrackingNo?: string
  offlineLogisticsNote?: string
}) {
  return request<{ success: boolean; message: string }>(
    `/task/${data.taskId}/ship`,
    {
      method: 'POST',
      body: JSON.stringify(data),
    },
  )
}

export async function uploadTaskResult(data: {
  taskId: number
  resultUrl: string
}) {
  return request<{ success: boolean; message: string }>(
    `/task/${data.taskId}/upload-result`,
    {
      method: 'POST',
      body: JSON.stringify(data),
    },
  )
}

// ====== 面料库 ======

export async function getFabricLibraryList(params: {
  supplierTenantId?: number
  category?: string
  onlyVisible?: boolean
  page?: number
  size?: number
}) {
  const query = new URLSearchParams(
    params as any,
  ).toString()
  return request<{
    fabrics: FabricInfo[]
    total: number
    page: number
    size: number
  }>(`/fabric-library/list?${query}`)
}

export async function getFabricDetail(fabricId: number) {
  return request<FabricInfo>(`/fabric-library/${fabricId}`)
}

export async function createFabric(data: {
  name: string
  category?: string
  images?: string[]
  videoUrl?: string
  specs?: Record<string, any>
  pricePerMeter?: number
  stockStatus?: string
}) {
  return request<{ success: boolean; message: string }>(
    '/fabric-library',
    {
      method: 'POST',
      body: JSON.stringify(data),
    },
  )
}

export async function updateFabric(fabricId: number, data: {
  name?: string
  category?: string
  images?: string[]
  videoUrl?: string
  specs?: Record<string, any>
  pricePerMeter?: number
  stockStatus?: string
  isVisible?: number
}) {
  return request<{ success: boolean; message: string }>(
    `/fabric-library/${fabricId}`,
    {
      method: 'PUT',
      body: JSON.stringify(data),
    },
  )
}

export async function deleteFabric(fabricId: number) {
  return request<{ success: boolean; message: string }>(
    `/fabric-library/${fabricId}`,
    {
      method: 'DELETE',
    },
  )
}

// ====== 消息 ======

export async function getMessageList(params: {
  type?: string
  unreadOnly?: boolean
  page?: number
  size?: number
}) {
  const query = new URLSearchParams(
    params as any,
  ).toString()
  return request<{
    messages: MessageInfo[]
    total: number
    page: number
    size: number
  }>(`/message/list?${query}`)
}

export async function markMessageRead(messageId: number) {
  return request<{ success: boolean; message: string }>(
    `/message/${messageId}/read`,
    {
      method: 'PUT',
    },
  )
}

export async function getUnreadCount() {
  return request<number>('/message/unread-count')
}

// ====== 余额 ======

export async function getBalance() {
  return request<{ balance: number; frozen: number; total: number }>(
    'http://localhost:8080/api/v1/account/balance',
  )
}