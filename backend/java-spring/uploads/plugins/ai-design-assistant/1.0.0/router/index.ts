import type { RouteRecordRaw } from 'vue-router'

// 插件内所有页面
const PluginLayout = () => import('../pages/PluginLayout.vue')
const Create = () => import('../pages/create.vue')
const PendingList = () => import('../pages/pending-list.vue')
const AssistantDetail = () => import('../pages/assistant-detail.vue')
const MyTasks = () => import('../pages/my-tasks.vue')
const TaskBoard = () => import('../pages/board.vue')
const FabricManage = () => import('../pages/fabric-manage.vue')
const MessageList = () => import('../pages/message-list.vue')
const TeamSettings = () => import('../pages/team-settings.vue')
const PartnerManage = () => import('../pages/partner-manage.vue')

export const pluginRoutes: RouteRecordRaw[] = [
  {
    path: '/plugins/ai-design-assistant',
    component: () => import('../pages/PluginLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/plugins/ai-design-assistant/list',
      },
      {
        path: 'list',
        name: 'AiDesignAssistantList',
        component: () => import('../pages/requirement-list.vue'),
        meta: { title: '我的设计需求', requiresAuth: true },
      },
      {
        path: 'create',
        name: 'AiDesignAssistantCreate',
        component: Create,
        meta: { title: '创建设计需求', requiresAuth: true },
      },
      {
        path: 'pending',
        name: 'AiDesignAssistantPending',
        component: PendingList,
        meta: { title: '设计助理待办', requiresAuth: true },
      },
      {
        path: 'detail/:id',
        name: 'AiDesignAssistantDetail',
        component: AssistantDetail,
        meta: { title: '需求复核与任务编辑', requiresAuth: true },
      },
      {
        path: 'tasks',
        name: 'AiDesignAssistantTasks',
        component: MyTasks,
        meta: { title: '我的任务', requiresAuth: true },
      },
      {
        path: 'board',
        name: 'AiDesignAssistantBoard',
        component: TaskBoard,
        meta: { title: '任务看板', requiresAuth: true },
      },
      {
        path: 'fabrics',
        name: 'AiDesignAssistantFabrics',
        component: FabricManage,
        meta: { title: '面料库管理', requiresAuth: true },
      },
      {
        path: 'messages',
        name: 'AiDesignAssistantMessages',
        component: MessageList,
        meta: { title: '消息中心', requiresAuth: true },
      },
      {
        path: 'settings',
        name: 'AiDesignAssistantSettings',
        component: TeamSettings,
        meta: { title: '成员管理', requiresAuth: true, roles: ['boss'] },
      },
      {
        path: 'partners',
        name: 'AiDesignAssistantPartners',
        component: PartnerManage,
        meta: { title: '合作方管理', requiresAuth: true, roles: ['boss'] },
      },
    ],
  },
]

export default pluginRoutes