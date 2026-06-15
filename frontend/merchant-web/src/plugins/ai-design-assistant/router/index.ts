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
const GuidePage = () => import('../pages/guide-page.vue')

// 身份选择路由（独立，不在 PluginLayout 内）
const identitySelectRoute: RouteRecordRaw = {
  path: '/plugins/ai-design-assistant/identity',
  name: 'AiDesignAssistantIdentity',
  component: IdentitySelect,
  meta: { title: '选择身份', requiresAuth: true },
}

// boss 角色互认列表
const BOSS_ROLES = ['boss', 'merchant_owner', 'tenant_admin'];

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
          // boss 默认跳转到管理看板，设计师跳转到我的设计需求，其他角色跳转到我的任务
          const identityRole = localStorage.getItem('ai_design_role')
          if (identityRole && BOSS_ROLES.includes(identityRole)) {
            return '/plugins/ai-design-assistant/board'
          }
          if (identityRole === 'designer' || identityRole === 'merchant_editor') {
            return '/plugins/ai-design-assistant/list'
          }
          return '/plugins/ai-design-assistant/tasks'
        },
      },
      {
        path: 'list',
        name: 'AiDesignAssistantList',
        component: () => import('../pages/requirement-list.vue'),
        meta: { title: '我的设计需求', requiresAuth: true, requiresIdentity: true, roles: ['designer', 'merchant_editor'] },
      },
      {
        path: 'create',
        name: 'AiDesignAssistantCreate',
        component: Create,
        meta: { title: '创建设计需求', requiresAuth: true, requiresIdentity: true, roles: ['designer', 'merchant_editor'] },
      },
      {
        path: 'pending',
        name: 'AiDesignAssistantPending',
        component: PendingList,
        meta: { title: '设计助理待办', requiresAuth: true, requiresIdentity: true, roles: ['design_assistant', 'merchant_editor'] },
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
        meta: { title: '我的任务', requiresAuth: true, requiresIdentity: true, roles: ['designer', 'pattern_maker', 'operator', 'viewer', 'merchant_editor', 'merchant_operator', 'merchant_viewer'] },
      },
      {
        path: 'board',
        name: 'AiDesignAssistantBoard',
        component: TaskBoard,
        meta: { title: '管理看板', requiresAuth: true, requiresIdentity: true, roles: ['boss', 'merchant_owner', 'tenant_admin'] },
      },
      {
        path: 'fabrics',
        name: 'AiDesignAssistantFabrics',
        component: FabricManage,
        meta: { title: '面料库管理', requiresAuth: true, requiresIdentity: true, roles: ['operator', 'merchant_operator', 'tenant_operator'] },
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
        meta: { title: '成员管理', requiresAuth: true, requiresIdentity: true, roles: ['boss', 'merchant_owner', 'tenant_admin'] },
      },
      {
        path: 'partners',
        name: 'AiDesignAssistantPartners',
        component: PartnerManage,
        meta: { title: '合作方管理', requiresAuth: true, requiresIdentity: true, roles: ['boss', 'merchant_owner', 'tenant_admin'] },
      },
      // 引导页：管理员访问非自有权限路由时展示
      {
        path: 'guide',
        name: 'AiDesignAssistantGuide',
        component: GuidePage,
        meta: { title: '功能引导', requiresAuth: true, requiresIdentity: true },
      },
    ],
  },
]

/**
 * 安装身份守卫 - 在进入插件页面时检查是否已选择身份
 * 管理员访问非自有权限路由时，跳转到引导页（而非直接拒绝）
 * 普通角色访问非自有权限路由时，跳转到默认页
 */
export function installIdentityGuard(router: Router) {
  router.beforeEach((to, from, next) => {
    // 只处理 AI 设计助手插件的路由
    if (!to.path.startsWith('/plugins/ai-design-assistant')) {
      return next()
    }

    // 如果正在前往身份选择页或引导页，直接放行
    if (to.path === '/plugins/ai-design-assistant/identity' || to.path === '/plugins/ai-design-assistant/guide') {
      return next()
    }

    // 检查是否已选择身份
    const identityPrefix = localStorage.getItem('ai_design_identity_prefix')
    const identityTenantId = localStorage.getItem('ai_design_tenant_id')
    const identityRole = localStorage.getItem('ai_design_role')

    if (!identityPrefix || !identityTenantId || !identityRole) {
      return next('/plugins/ai-design-assistant/identity')
    }

    // 统一基于路由 meta.roles 做权限校验
    const routeRoles = to.meta?.roles as string[] | undefined
    if (routeRoles && routeRoles.length > 0 && identityRole) {
      // 直接匹配
      if (routeRoles.includes(identityRole)) {
        return next()
      }
      // boss / merchant_owner / tenant_admin 三类管理员互认
      if (BOSS_ROLES.includes(identityRole) && routeRoles.some(r => BOSS_ROLES.includes(r))) {
        return next()
      }
      // 角色互认映射：简化角色 ↔ 后端角色
      const roleAliasMap: Record<string, string[]> = {
        'designer': ['merchant_editor'],
        'design_assistant': ['merchant_editor'],
        'pattern_maker': ['merchant_editor'],
        'operator': ['merchant_operator', 'tenant_operator'],
        'viewer': ['merchant_viewer', 'tenant_viewer'],
      }
      const aliases = roleAliasMap[identityRole] || []
      if (routeRoles.some(r => aliases.includes(r))) {
        return next()
      }
      // 权限不足
      if (BOSS_ROLES.includes(identityRole)) {
        // 管理员 → 跳转到引导页，告诉用户需要什么角色
        return next(`/plugins/ai-design-assistant/guide?target=${encodeURIComponent(to.path)}`)
      }
      // 普通角色 → 根据角色跳转到默认页
      if (identityRole === 'designer' || identityRole === 'merchant_editor') {
        return next('/plugins/ai-design-assistant/list')
      }
      return next('/plugins/ai-design-assistant/tasks')
    }

    // 没有 roles 限制的路由（如 messages、detail）放行
    next()
  })
}

export default pluginRoutes