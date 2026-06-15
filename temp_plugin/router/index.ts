import type { RouteRecordRaw } from 'vue-router'
import type { Router } from 'vue-router'

// 插件内所有页面
const PluginLayout = () => import('../pages/PluginLayout.vue')
const IdentitySelect = () => import('../pages/identity-select.vue')
const Create = () => import('../pages/create.vue')
const PendingList = () => import('../pages/pending-list.vue')
const AssistantDetail = () => import('../pages/assistant-detail.vue')
const MyTasks = () => import('../pages/my-tasks.vue')
const TaskBoard = () => import('../pages/board.vue')
const FabricManage = () => import('../pages/fabric-manage.vue')
const MessageList = () => import('../pages/message-list.vue')
const TeamSettings = () => import('../pages/team-settings.vue')
const PartnerManage = () => import('../pages/partner-manage.vue')

// 身份选择路由（独立，不在 PluginLayout 内）
const identitySelectRoute: RouteRecordRaw = {
  path: '/plugins/ai-design-assistant/identity',
  name: 'AiDesignAssistantIdentity',
  component: IdentitySelect,
  meta: { title: '选择身份', requiresAuth: true },
}

export const pluginRoutes: RouteRecordRaw[] = [
  identitySelectRoute,
  {
    path: '/plugins/ai-design-assistant',
    component: () => import('../pages/PluginLayout.vue'),
    meta: { requiresAuth: true, requiresIdentity: true },
    children: [
      {
        path: '',
        redirect: (to: any) => {
          // boss 默认跳转到管理看板，其他角色跳转到需求列表
          const identityRole = localStorage.getItem('ai_design_role')
          if (identityRole === 'boss') {
            return '/plugins/ai-design-assistant/board'
          }
          return '/plugins/ai-design-assistant/list'
        },
      },
      {
        path: 'list',
        name: 'AiDesignAssistantList',
        component: () => import('../pages/requirement-list.vue'),
        meta: { title: '我的设计需求', requiresAuth: true, requiresIdentity: true },
      },
      {
        path: 'create',
        name: 'AiDesignAssistantCreate',
        component: Create,
        meta: { title: '创建设计需求', requiresAuth: true, requiresIdentity: true, roles: ['designer'] },
      },
      {
        path: 'pending',
        name: 'AiDesignAssistantPending',
        component: PendingList,
        meta: { title: '设计助理待办', requiresAuth: true, requiresIdentity: true, roles: ['design_assistant'] },
      },
      {
        path: 'detail/:id',
        name: 'AiDesignAssistantDetail',
        component: AssistantDetail,
        meta: { title: '需求复核与任务编辑', requiresAuth: true, requiresIdentity: true },
      },
      {
        path: 'tasks',
        name: 'AiDesignAssistantTasks',
        component: MyTasks,
        meta: { title: '我的任务', requiresAuth: true, requiresIdentity: true, roles: ['designer', 'pattern_maker', 'operator', 'viewer'] },
      },
      {
        path: 'board',
        name: 'AiDesignAssistantBoard',
        component: TaskBoard,
        meta: { title: '管理看板', requiresAuth: true, requiresIdentity: true, roles: ['boss'] },
      },
      {
        path: 'fabrics',
        name: 'AiDesignAssistantFabrics',
        component: FabricManage,
        meta: { title: '面料库管理', requiresAuth: true, requiresIdentity: true, roles: ['operator'] },
      },
      {
        path: 'messages',
        name: 'AiDesignAssistantMessages',
        component: MessageList,
        meta: { title: '消息中心', requiresAuth: true, requiresIdentity: true },
      },
      {
        path: 'settings',
        name: 'AiDesignAssistantSettings',
        component: TeamSettings,
        meta: { title: '成员管理', requiresAuth: true, requiresIdentity: true, roles: ['boss'] },
      },
      {
        path: 'partners',
        name: 'AiDesignAssistantPartners',
        component: PartnerManage,
        meta: { title: '合作方管理', requiresAuth: true, requiresIdentity: true, roles: ['boss'] },
      },
    ],
  },
]

/**
 * 安装身份守卫 - 在进入插件页面时检查是否已选择身份
 * 应在 install() 函数中调用
 * 同时检查角色权限，boss 角色默认跳转到管理看板
 */
export function installIdentityGuard(router: Router) {
  router.beforeEach((to, from, next) => {
    // 只处理 AI 设计助手插件的路由
    if (!to.path.startsWith('/plugins/ai-design-assistant')) {
      return next()
    }

    // 如果正在前往身份选择页，直接放行
    if (to.path === '/plugins/ai-design-assistant/identity') {
      return next()
    }

    // 检查是否已选择身份
    const identityPrefix = localStorage.getItem('ai_design_identity_prefix')
    const identityTenantId = localStorage.getItem('ai_design_tenant_id')
    const identityRole = localStorage.getItem('ai_design_role')

    if (!identityPrefix || !identityTenantId || !identityRole) {
      // 未选择身份，跳转到身份选择页
      return next('/plugins/ai-design-assistant/identity')
    }

    // boss 角色特殊处理：默认跳转到管理看板
    if (identityRole === 'boss') {
      // boss 只能访问管理看板、成员管理、合作方管理、消息中心
      const bossAllowedPaths = [
        '/plugins/ai-design-assistant/board',
        '/plugins/ai-design-assistant/settings',
        '/plugins/ai-design-assistant/partners',
        '/plugins/ai-design-assistant/messages',
        '/plugins/ai-design-assistant/detail',
        '/plugins/ai-design-assistant/identity',
      ]
      const isBossAllowed = bossAllowedPaths.some(p => to.path.startsWith(p))
      if (!isBossAllowed) {
        // boss 不能访问其他角色页面，重定向到管理看板
        return next('/plugins/ai-design-assistant/board')
      }
    }

    // 非 boss 角色不能访问 boss 专属页面
    const bossOnlyPaths = ['/plugins/ai-design-assistant/settings', '/plugins/ai-design-assistant/partners']
    if (identityRole !== 'boss' && bossOnlyPaths.some(p => to.path.startsWith(p))) {
      return next('/plugins/ai-design-assistant/list')
    }

    // 检查路由的 roles meta
    const routeRoles = to.meta?.roles as string[] | undefined
    if (routeRoles && routeRoles.length > 0 && identityRole) {
      if (!routeRoles.includes(identityRole)) {
        // 角色不允许访问此页面，跳转到对应默认页
        if (identityRole === 'boss') {
          return next('/plugins/ai-design-assistant/board')
        }
        return next('/plugins/ai-design-assistant/list')
      }
    }

    next()
  })
}

export default pluginRoutes
