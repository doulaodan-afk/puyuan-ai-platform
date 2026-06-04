import type { RouteRecordRaw } from "vue-router";

export type AdminRole =
  | "platform_super_admin"
  | "platform_ops"
  | "platform_finance"
  | "platform_auditor";

export interface AdminRouteMeta extends Record<string, unknown> {
  title: string;
  requiresAuth: boolean;
  roles?: AdminRole[];
}

export const adminRoutes: RouteRecordRaw[] = [
  {
    path: "/",
    redirect: "/admin",
  },
  {
    path: "/admin/login",
    name: "AdminLogin",
    component: () => import("../pages/AdminLoginPage.vue"),
    meta: { title: "管理登录", requiresAuth: false },
  },
  {
    path: "/admin/forbidden",
    name: "AdminForbidden",
    component: () => import("../pages/AdminForbiddenPage.vue"),
    meta: { title: "无权访问", requiresAuth: false },
  },
  {
    path: "/admin",
    redirect: "/admin/dashboard",
  },
  {
    path: "/admin/dashboard",
    name: "AdminDashboard",
    component: () => import("../pages/AdminDashboardPage.vue"),
    meta: {
      title: "运营看板",
      requiresAuth: true,
      roles: ["platform_super_admin", "platform_ops", "platform_finance", "platform_auditor"],
    },
  },
  {
    path: "/admin/tenants",
    name: "AdminTenants",
    component: () => import("../pages/AdminTenantsPage.vue"),
    meta: {
      title: "租户管理",
      requiresAuth: true,
      roles: ["platform_super_admin", "platform_ops"],
    },
  },
  {
    path: "/admin/plugins",
    name: "AdminPlugins",
    component: () => import("../pages/AdminPluginsPage.vue"),
    meta: {
      title: "插件管理",
      requiresAuth: true,
      roles: ["platform_super_admin", "platform_ops"],
    },
  },
  {
    path: "/admin/plugins/sandbox/:pluginId",
    name: "AdminPluginSandbox",
    component: () => import("../pages/AdminPluginSandboxPage.vue"),
    meta: {
      title: "沙箱测试",
      requiresAuth: true,
      roles: ["platform_super_admin", "platform_ops"],
    },
  },
  {
    path: "/admin/pricing",
    name: "AdminPricing",
    component: () => import("../pages/AdminPricingPage.vue"),
    meta: {
      title: "定价管理",
      requiresAuth: true,
      roles: ["platform_super_admin", "platform_finance"],
    },
  },
  {
    path: "/admin/billing",
    name: "AdminBilling",
    component: () => import("../pages/AdminBillingPage.vue"),
    meta: {
      title: "账单管理",
      requiresAuth: true,
      roles: ["platform_super_admin", "platform_finance"],
    },
  },
  {
    path: "/admin/supplier-review",
    name: "AdminSupplierReview",
    component: () => import("../pages/AdminSupplierReviewPage.vue"),
    meta: {
      title: "面料商入驻审核",
      requiresAuth: true,
      roles: ["platform_super_admin", "platform_ops"],
    },
  },
  {
    path: "/admin/audit",
    name: "AdminAudit",
    component: () => import("../pages/AdminAuditPage.vue"),
    meta: {
      title: "审计日志",
      requiresAuth: true,
      roles: ["platform_super_admin", "platform_auditor"],
    },
  },
  {
    path: "/admin/system-config",
    name: "AdminSystemConfig",
    component: () => import("../pages/AdminSystemConfigPage.vue"),
    meta: {
      title: "对象存储",
      requiresAuth: true,
      roles: ["platform_super_admin"],
    },
  },
  {
    path: "/admin/ai-config/providers",
    name: "AdminAiProviders",
    component: () => import("../pages/AdminAiProviderPage.vue"),
    meta: {
      title: "AI 提供商",
      requiresAuth: true,
      roles: ["platform_super_admin", "platform_ops"],
      parentMenu: "AI 配置",
    },
  },
  {
    path: "/admin/ai-config/scenes",
    name: "AdminAiScenes",
    component: () => import("../pages/AdminAiScenePage.vue"),
    meta: {
      title: "场景模型",
      requiresAuth: true,
      roles: ["platform_super_admin", "platform_ops"],
      parentMenu: "AI 配置",
    },
  },
];
