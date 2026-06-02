import type { RouteRecordRaw } from "vue-router";

export type MerchantRole =
  | "merchant_owner"
  | "merchant_operator"
  | "merchant_editor"
  | "merchant_viewer"
  | "boss"
  | "tenant_admin"
  | "tenant_operator"
  | "tenant_viewer";

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
];
  