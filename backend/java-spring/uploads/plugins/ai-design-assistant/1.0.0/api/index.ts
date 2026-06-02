// AI 设计助手 API 客户端
// 所有请求路径统一使用 /api/plugins/ai-design-assistant/* 前缀
// 支持 VITE_USE_MOCK=true 模式

const API_BASE = '/api/plugins/ai-design-assistant'
const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

// 获取请求头
function getHeaders() {
  const tenantId = localStorage.getItem('tenantId') || '2001'
  const userId = localStorage.getItem('userId') || '1'
  return {
    'X-Tenant-Id': tenantId,
    'X-User-Id': userId,
    'Content-Type': 'application/json',
  }
}

// 通用请求函数
async function request<T>(url: string, options: RequestInit = {}): Promise<T> {
  if (USE_MOCK) {
    return mockRequest<T>(url, options)
  }

  const response = await fetch(`${API_BASE}${url}`, {
    ...options,
    headers: { ...getHeaders(), ...options.headers },
  })

  const result = await response.json()

  if (result.code !== 200) {
    throw new Error(result.message || '请求失败')
  }

  return result.data as T
}

// ====== Mock 数据 ======

const mockRequirements = [
  { id: 1, title: '2026秋季毛衣设计需求', status: 'released', totalTokenCost: 1200, createdAt: '2026-05-20T10:00:00Z', taskCount: 3 },
  { id: 2, title: '羊毛大衣AI设计', status: 'completed', totalTokenCost: 800, createdAt: '2026-05-18T14:30:00Z', taskCount: 2 },
  { id: 3, title: '新款针织衫需求', status: 'draft', totalTokenCost: 0, createdAt: '2026-05-22T09:00:00Z', taskCount: 0 },
]

const mockTasks = [
  { id: 1, requirementId: 1, taskType: 'fabric', assigneeType: 'supplier', assigneeId: 10, status: 'done', deadline: '2026-05-25T00:00:00Z', resultUrl: 'https://example.com/fabric1.jpg', canAccept: false, requirementTitle: '2026秋季毛衣设计需求' },
  { id: 2, requirementId: 1, taskType: 'pattern', assigneeType: 'pattern_service', assigneeId: 11, status: 'pending', deadline: '2026-05-28T00:00:00Z', resultUrl: null, canAccept: true, requirementTitle: '2026秋季毛衣设计需求' },
]

const mockFabrics = [
  { id: 1, supplierTenantId: 10, name: '高品质羊毛面料', category: '羊毛', images: ['https://picsum.photos/400/300?fabric=1'], pricePerMeter: 120, stockStatus: 'in_stock', isVisible: 1 },
  { id: 2, supplierTenantId: 10, name: '棉麻混纺面料', category: '棉麻', images: ['https://picsum.photos/400/300?fabric=2'], pricePerMeter: 85, stockStatus: 'in_stock', isVisible: 1 },
]

const mockMessages = [
  { id: 1, receiverId: 1, senderId: 2, title: '任务完成通知', content: '面料任务已完成，请查看结果', type: 'task', isRead: false, relatedId: 1, createdAt: '2026-05-22T08:00:00Z' },
]

async function mockRequest<T>(url: string, options: RequestInit = {}): Promise<T> {
  await new Promise(resolve => setTimeout(resolve, 100))

  if (url.startsWith('/requirements') && options.method === 'GET') {
    return { list: mockRequirements, total: mockRequirements.length } as any as T
  }
  if (url.match(/\/requirements\/\d+$/) && !url.includes('/ai-summary') && options.method === 'GET') {
    const id = parseInt(url.split('/').pop()!)
    return { requirement: mockRequirements.find(r => r.id === id) || mockRequirements[0] } as any as T
  }
  if (url.includes('/tasks') && options.method === 'GET') {
    return { tasks: mockTasks, total: mockTasks.length } as any as T
  }
  if (url.includes('/fabrics') && options.method === 'GET') {
    return { fabrics: mockFabrics, total: mockFabrics.length } as any as T
  }
  if (url.includes('/messages') && options.method === 'GET') {
    return { messages: mockMessages, total: mockMessages.length } as any as T
  }
  if (url.includes('/statistics')) {
    return {
      requirements: { total: 5, draft: 1, released: 2, completed: 2 },
      tasks: { total: 10, pending: 3, accepted: 2, done: 5 },
      fabrics: { total: 8, in_stock: 6, out_of_stock: 2 },
      messages: { total: 6, unread: 2 }
    } as any as T
  }

  return {} as T
}

// ====== 类型复用 ======

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
  rawAudioUrl: string | null
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
  return request<RequirementDetail>('/requirements', {
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
  }>('/requirements/chat', {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

export async function summarize(requirementId: number) {
  return request<{
    aiSummary: string
    summaryData: Record<string, any>
  }>(`/requirements/${requirementId}/ai-summary`, {
    method: 'GET',
  })
}

export async function confirmRequirement(requirementId: number) {
  return request<{ success: boolean; message: string }>(
    `/requirements/${requirementId}`,
    {
      method: 'PUT',
      body: JSON.stringify({ designerApproved: 1 }),
    },
  )
}

export async function transferToAssistant(data: {
  requirementId: number
  assistantId?: number
}) {
  return request<{ success: boolean; message: string }>(
    `/requirements/${data.requirementId}`,
    {
      method: 'PUT',
      body: JSON.stringify({ status: 'assistant_processing' }),
    },
  )
}

export async function getRequirementList(params: {
  status?: string
  page?: number
  size?: number
}) {
  const query = new URLSearchParams(params as any).toString()
  return request<{ list: RequirementListItem[]; total: number }>(
    `/requirements?${query}`,
  )
}

export async function getRequirementDetail(id: number) {
  return request<{ requirement: RequirementDetail }>(`/requirements/${id}`).then(r => r.requirement)
}

// ====== 任务管理 ======

export async function editTask(data: {
  taskId: number
  assigneeId?: number
  deadline?: string
  content?: Record<string, any>
}) {
  return request<{ success: boolean; message: string }>(
    `/tasks/${data.taskId}`,
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
    '/tasks',
    {
      method: 'POST',
      body: JSON.stringify(data),
    },
  )
}

export async function deleteTask(taskId: number) {
  return request<{ success: boolean; message: string }>(
    `/tasks/${taskId}`,
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
    `/requirements/${requirementId}`,
    {
      method: 'PUT',
      body: JSON.stringify({ status: 'released' }),
    },
  )
}

export async function getMyTasks(params: {
  status?: string
  taskType?: string
  page?: number
  size?: number
}) {
  const query = new URLSearchParams(params as any).toString()
  return request<{
    tasks: TaskInfo[]
    total: number
    page: number
    size: number
  }>(`/tasks?${query}`)
}

export async function getTaskDetail(taskId: number) {
  return request<TaskInfo>(`/tasks/${taskId}`)
}

export async function updateTaskStatus(data: {
  taskId: number
  status: string
  rejectReason?: string
}) {
  if (data.status === 'accepted') {
    return request<{ success: boolean; message: string }>(`/tasks/${data.taskId}/accept`, { method: 'POST' })
  }
  if (data.status === 'rejected') {
    return request<{ success: boolean; message: string }>(`/tasks/${data.taskId}/reject?reason=${data.rejectReason || ''}`, { method: 'POST' })
  }
  return request<{ success: boolean; message: string }>(
    `/tasks/${data.taskId}`,
    {
      method: 'PUT',
      body: JSON.stringify({ status: data.status }),
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
    `/tasks/${data.taskId}/ship`,
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
    `/tasks/${data.taskId}`,
    {
      method: 'PUT',
      body: JSON.stringify({ resultUrl: data.resultUrl, status: 'done' }),
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
  const query = new URLSearchParams(params as any).toString()
  return request<{
    fabrics: FabricInfo[]
    total: number
    page: number
    size: number
  }>(`/fabrics?${query}`)
}

export async function getFabricDetail(fabricId: number) {
  return request<FabricInfo>(`/fabrics/${fabricId}`)
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
    '/fabrics',
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
    `/fabrics/${fabricId}`,
    {
      method: 'PUT',
      body: JSON.stringify(data),
    },
  )
}

export async function deleteFabric(fabricId: number) {
  return request<{ success: boolean; message: string }>(
    `/fabrics/${fabricId}`,
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
  const query = new URLSearchParams(params as any).toString()
  return request<{
    messages: MessageInfo[]
    total: number
    page: number
    size: number
  }>(`/messages?userId=1&${query}`)
}

export async function markMessageRead(messageId: number) {
  return request<{ success: boolean; message: string }>(
    `/messages/${messageId}/read`,
    {
      method: 'POST',
    },
  )
}

export async function getUnreadCount() {
  return request<number>('/messages/unread-count')
}

// ====== 统计数据 ======

export async function getStatistics() {
  return request<{
    requirements: { total: number; draft: number; released: number; completed: number }
    tasks: { total: number; pending: number; accepted: number; done: number }
    fabrics: { total: number; in_stock: number; out_of_stock: number }
    messages: { total: number; unread: number }
  }>('/statistics?tenantId=1')
}

// ====== 余额 ======

export async function getBalance() {
  return request<{ balance: number; frozen: number; total: number }>(
    '/api/v1/account/balance',
  )
}