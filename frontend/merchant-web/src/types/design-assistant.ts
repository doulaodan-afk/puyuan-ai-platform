// 需求状态
export type RequirementStatus = 'draft' | 'assistant_processing' | 'released' | 'completed' | 'cancelled'

// 任务状态
export type TaskStatus = 'draft' | 'pending' | 'accepted' | 'shipped' | 'delivered' | 'rejected' | 'done' | 'cancelled'

// 任务类型
export type TaskType = 'fabric' | 'pattern'

// 对话消息
export interface ChatMessage {
  role: 'user' | 'assistant' | 'system'
  content: string
  time: string
}

// 面料规格
export interface FabricSpec {
  type?: string
  weight?: string
  color?: string
  special_requirements?: string
}

// 版型规格
export interface PatternSpec {
  collar?: string
  sleeve?: string
  waist?: string
  silhouette?: string
  other_details?: string
}

// AI 总结
export interface AiSummary {
  fabric: FabricSpec
  pattern: PatternSpec
  quantity?: number
  deadline?: string
}

// 设计需求列表项
export interface RequirementListItem {
  id: number
  title: string
  status: RequirementStatus
  totalTokenCost: number
  createdAt: string
  taskCount: number
}

// 设计需求详情
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
  designerApproved: 0 | 1 | 2  // 0-未确认,1-确认发布,2-转助理
  assistantId: number
  status: RequirementStatus
  totalTokenCost: number
  createdAt: string
  updatedAt: string
  tasks: TaskInfo[]
}

// 子任务信息
export interface TaskInfo {
  id: number
  requirementId: number
  taskType: TaskType
  assigneeType: 'supplier' | 'pattern_service' | 'internal'
  assigneeId: number
  assigneeName?: string
  content: FabricSpec | PatternSpec | Record<string, any>
  status: TaskStatus
  deadline: string | null
  resultUrl: string | null
  fabricTaskId: number
  logisticsCompany: string | null
  logisticsTrackingNo: string | null
  logisticsStatus: 'pending' | 'shipped' | 'delivered'
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

// 面料库项
export interface FabricLibraryItem {
  id: number
  supplierTenantId: number
  name: string
  category: string | null
  images: string[]
  videoUrl: string | null
  specs: FabricSpec | Record<string, any>
  pricePerMeter: number | null
  stockStatus: 'in_stock' | 'out_of_stock'
  isVisible: number
  createdAt: string
  updatedAt: string
}

// 分配规则
export interface TaskAssignRule {
  id: number
  ruleName: string
  keyword: string
  targetTenantId: number
  targetTenantName?: string
  taskType: TaskType
  priority: number
  enabled: number
  createdAt: string
  updatedAt: string
}

// 站内信
export interface MessageInfo {
  id: number
  receiverId: number
  senderId: number
  senderName?: string
  title: string
  content: string
  type: 'system' | 'task' | 'remind'
  isRead: boolean
  relatedId: number
  createdAt: string
}

// 租户统计
export interface TenantStatistics {
  totalRequirements: number
  draftCount: number
  processingCount: number
  releasedCount: number
  completedCount: number
  totalTokens: number
  totalTasks: number
  completedTasks: number
  taskCompletionRate: number
}

// 任务列表响应
export interface MyTasksResponse {
  tasks: TaskInfo[]
  total: number
  page: number
  size: number
}

// 通用响应
export interface CommonResponse {
  success: boolean
  message: string
  data?: any
}

// 站内信列表响应
export interface MessageListResponse {
  messages: MessageInfo[]
  total: number
  page: number
  size: number
}

// 面料库列表响应
export interface FabricLibraryListResponse {
  fabrics: FabricLibraryItem[]
  total: number
  page: number
  size: number
}