import type { RouteRecordRaw } from "vue-router";

export type MerchantRole =
  | "merchant_owner"
  | "merchant_operator"
  | "merchant_editor"
  | "merchant_viewer"
  | "boss"
  | "tenant_admin"
  | "tenant_operator"
  | "tenant_viewer"
  | "platform_super_admin";

export interface AppRouteMeta extends Record<string, unknown> {
  title: string;
  requiresAuth: boolean;
  roles?: MerchantRole[];
  allowWhenFrozen?: boolean;
}

export const merchantRoutes: RouteRecordRaw[] = [
  {
    path: "/login",
    name: "MerchantLogin",
    component: () => import("../pages/LoginPage.vue"),
    meta: { title: "登录", requiresAuth: false },
  },
  {
    path: "/forbidden",
    name: "MerchantForbidden",
    component: () => import("../pages/ForbiddenPage.vue"),
    meta: { title: "无权限", requiresAuth: false },
  },
  {
    path: "/",
    redirect: "/dashboard",
  },
  {
    path: "/dashboard",
    name: "MerchantDashboard",
    component: () => import("../pages/DashboardPage.vue"),
    meta: {
      title: "工作台",
      requiresAuth: true,
      roles: ["merchant_owner", "merchant_operator", "merchant_editor", "merchant_viewer"],
      allowWhenFrozen: false,
    },
  },
  {
    path: "/plugins",
    name: "MerchantPlugins",
    component: () => import("../pages/PluginsPage.vue"),
    meta: {
      title: "插件列表",
      requiresAuth: true,
      roles: ["merchant_owner", "merchant_operator", "merchant_editor", "merchant_viewer"],
    },
  },
  {
    path: "/plugins/ai-image",
    name: "MerchantPluginImage",
    component: () => import("../pages/PluginImagePage.vue"),
    meta: {
      title: "AI 图片生成",
      requiresAuth: true,
    },
  },
  {
    path: "/plugins/ai-script",
    name: "MerchantPluginScript",
    component: () => import("../pages/PluginScriptPage.vue"),
    meta: {
      title: "AI 脚本生成",
      requiresAuth: true,
    },
  },
  {
    path: "/plugins/ai-translate",
    name: "MerchantPluginTranslate",
    component: () => import("../pages/AiTranslate.vue"),
    meta: {
      title: "AI 跨境翻译",
      requiresAuth: true,
    },
  },
  {
    path: "/account/balance",
    name: "MerchantBalance",
    component: () => import("../pages/BalancePage.vue"),
    meta: {
      title: "账户余额",
      requiresAuth: true,
      roles: ["merchant_owner", "merchant_operator", "merchant_viewer"],
      allowWhenFrozen: true,
    },
  },
  {
    path: "/account/recharge",
    name: "MerchantRecharge",
    component: () => import("../pages/RechargePage.vue"),
    meta: {
      title: "充值中心",
      requiresAuth: true,
      roles: ["merchant_owner", "merchant_operator"],
      allowWhenFrozen: true,
    },
  },
  {
    path: "/account/ledger",
    name: "MerchantLedger",
    component: () => import("../pages/LedgerPage.vue"),
    meta: {
      title: "消费明细",
      requiresAuth: true,
      roles: ["merchant_owner", "merchant_operator", "merchant_viewer"],
      allowWhenFrozen: true,
    },
  },
  {
    path: "/billing",
    name: "MerchantBilling",
    component: () => import("../pages/BillingPage.vue"),
    meta: {
      title: "账单中心",
      requiresAuth: true,
      roles: ["merchant_owner", "merchant_operator", "merchant_viewer"],
      allowWhenFrozen: true,
    },
  },
  {
    path: "/settings",
    name: "MerchantSettings",
    component: () => import("../pages/SettingsPage.vue"),
    meta: {
      title: "设置",
      requiresAuth: true,
      roles: ["merchant_owner", "boss", "tenant_admin"],
    },
  },
  {
    path: "/profile",
    name: "MerchantProfile",
    component: () => import("../pages/ProfilePage.vue"),
    meta: {
      title: "个人中心",
      requiresAuth: true,
    },
  },
  {
    path: "/members",
    name: "MerchantMembers",
    component: () => import("../pages/MembersPage.vue"),
    meta: {
      title: "成员管理",
      requiresAuth: true,
      roles: ["merchant_owner", "boss", "tenant_admin"],
    },
  },
  // AI 设计助手 - 身份选择路由（独立，不在 PluginLayout 内）
  {
    path: "/plugins/ai-design-assistant/identity",
    name: "AiDesignAssistantIdentity",
    component: () => import("../plugins/ai-design-assistant/pages/identity-select.vue"),
    meta: { title: "选择身份", requiresAuth: true },
  },
  // AI 设计助手插件路由
  {
    path: "/plugins/ai-design-assistant",
    component: () => import("../plugins/ai-design-assistant/pages/PluginLayout.vue"),
    meta: { title: "AI 设计助手", requiresAuth: true },
    children: [
      {
        path: "",
        redirect: (to: any) => {
          // 首次访问（未选择过身份）→ 跳转到身份选择页
          const identityRole = localStorage.getItem('ai_design_role')
          if (!identityRole) {
            return '/plugins/ai-design-assistant/identity'
          }
          // boss 默认跳转到管理看板，其他角色跳转到需求列表
          if (identityRole === 'boss' || identityRole === 'merchant_owner' || identityRole === 'tenant_admin') {
            return '/plugins/ai-design-assistant/board'
          }
          return '/plugins/ai-design-assistant/list'
        },
      },
      {
        path: "list",
        name: "AiDesignAssistantList",
        component: () => import("../plugins/ai-design-assistant/pages/requirement-list.vue"),
        meta: { title: "我的设计需求", requiresAuth: true },
      },
      {
        path: "create",
        name: "AiDesignAssistantCreate",
        component: () => import("../plugins/ai-design-assistant/pages/create.vue"),
        meta: { title: "创建设计需求", requiresAuth: true, roles: ["designer", "merchant_editor"] },
      },
      {
        path: "pending",
        name: "AiDesignAssistantPending",
        component: () => import("../plugins/ai-design-assistant/pages/pending-list.vue"),
        meta: { title: "设计助理待办", requiresAuth: true, roles: ["design_assistant", "merchant_editor"] },
      },
      {
        path: "detail/:id",
        name: "AiDesignAssistantDetail",
        component: () => import("../plugins/ai-design-assistant/pages/assistant-detail.vue"),
        meta: { title: "需求复核与任务编辑", requiresAuth: true },
      },
      {
        path: "tasks",
        name: "AiDesignAssistantTasks",
        component: () => import("../plugins/ai-design-assistant/pages/my-tasks.vue"),
        meta: { title: "我的任务", requiresAuth: true, roles: ["designer", "pattern_maker", "operator", "viewer", "merchant_editor", "merchant_operator", "merchant_viewer"] },
      },
      {
        path: "board",
        name: "AiDesignAssistantBoard",
        component: () => import("../plugins/ai-design-assistant/pages/board.vue"),
        meta: { title: "管理看板", requiresAuth: true, roles: ["boss", "merchant_owner", "tenant_admin"] },
      },
      {
        path: "fabrics",
        name: "AiDesignAssistantFabrics",
        component: () => import("../plugins/ai-design-assistant/pages/fabric-manage.vue"),
        meta: { title: "面料库管理", requiresAuth: true, roles: ["operator", "merchant_operator", "tenant_operator"] },
      },
      {
        path: "messages",
        name: "AiDesignAssistantMessages",
        component: () => import("../plugins/ai-design-assistant/pages/message-list.vue"),
        meta: { title: "消息中心", requiresAuth: true },
      },
      {
        path: "settings",
        name: "AiDesignAssistantSettings",
        component: () => import("../plugins/ai-design-assistant/pages/team-settings.vue"),
        meta: { title: "成员管理", requiresAuth: true, roles: ["boss", "merchant_owner", "tenant_admin"] },
      },
      {
        path: "partners",
        name: "AiDesignAssistantPartners",
        component: () => import("../plugins/ai-design-assistant/pages/partner-manage.vue"),
        meta: { title: "合作方管理", requiresAuth: true, roles: ["boss", "merchant_owner", "tenant_admin"] },
      },
      // 引导页：管理员访问非自有权限路由时展示
      {
        path: "guide",
        name: "AiDesignAssistantGuide",
        component: () => import("../plugins/ai-design-assistant/pages/guide-page.vue"),
        meta: { title: "功能引导", requiresAuth: true },
      },
    ],
  },
];
  