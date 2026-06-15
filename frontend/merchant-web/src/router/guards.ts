import type { Router } from "vue-router";
import type { AppRouteMeta } from "./routes";
import { useAuthStore } from "../stores/auth";

function hasRoutePermission(roleCode: string | null, roles?: string[]): boolean {
  if (!roles || roles.length === 0) {
    return true;
  }
  if (!roleCode) {
    console.log("[Router] No roleCode, denying access");
    return false;
  }

  console.log("[Router] Checking permission for roleCode:", roleCode, "required roles:", roles);

  // Direct match - if user's role is in the allowed roles, allow
  if (roles.includes(roleCode)) {
    console.log("[Router] Direct role match found");
    return true;
  }

  // Map backend roles to frontend roles (bidirectional)
  const roleMap: Record<string, string[]> = {
    'boss': ['merchant_owner', 'boss'],
    'merchant_owner': ['boss', 'merchant_owner'],
    'tenant_admin': ['merchant_owner', 'tenant_admin'],
    'tenant_operator': ['merchant_operator', 'tenant_operator'],
    'tenant_viewer': ['merchant_viewer', 'tenant_viewer'],
    // Super admin can access all pages
    'platform_super_admin': ['merchant_owner', 'merchant_operator', 'merchant_editor', 'merchant_viewer', 'boss', 'tenant_admin', 'tenant_operator', 'tenant_viewer', 'platform_super_admin'],
    // AI 设计助手插件简化角色 → 后端角色的映射
    'designer': ['merchant_editor', 'designer'],
    'design_assistant': ['merchant_editor', 'design_assistant'],
    'pattern_maker': ['merchant_editor', 'pattern_maker'],
    'operator': ['merchant_operator', 'operator'],
    'viewer': ['merchant_viewer', 'viewer'],
  };

  const mappedRoles = roleMap[roleCode] || [roleCode];
  console.log("[Router] Mapped roles:", mappedRoles);

  return roles.some(role => mappedRoles.includes(role));
}

export function setupRouterGuards(router: Router): void {
  router.beforeEach(async (to) => {
    const auth = useAuthStore();
    const meta = (to.meta ?? {}) as Partial<AppRouteMeta>;

    console.log("[Router] Navigating to:", to.path);
    console.log("[Router] requiresAuth:", meta.requiresAuth, "profileLoaded:", auth.profileLoaded, "roleCode:", auth.roleCode);

    // Sandbox mode: use sandbox query params to set auth context
    if (to.query.sandbox === "true" && to.query.tenant_id && to.query.plugin_id) {
      console.log("[Router] Sandbox mode detected, tenant_id:", to.query.tenant_id, "plugin_id:", to.query.plugin_id);
      const sandboxTenantId = String(to.query.tenant_id);
      // Token format: token-{userId}-{tenantId} — userId=1 for sandbox operator
      const sandboxToken = `token-1-${sandboxTenantId}`;
      auth.setAccessToken(sandboxToken);
      auth.setTenantId(sandboxTenantId);
      auth.roleCode = "boss";
      auth.profileLoaded = true;
      auth.tenantStatus = 1;
      auth.isSandbox = true;
      return true;
    }

    // Already in sandbox mode (query params lost on subsequent navigations)
    if (auth.isSandbox) {
      return true;
    }

    if (!meta.requiresAuth) {
      return true;
    }

    if (!auth.accessToken) {
      console.log("[Router] No access token, redirect to login");
      return { path: "/login", query: { redirect: to.fullPath } };
    }

    if (!auth.profileLoaded) {
      try {
        console.log("[Router] Loading profile...");
        await auth.loadProfile();
        console.log("[Router] Profile loaded, roleCode:", auth.roleCode);
      } catch (e) {
        console.log("[Router] Profile load failed:", e);
        auth.clearAuth();
        return { path: "/login", query: { redirect: to.fullPath } };
      }
    }

    if (auth.tenantStatus === 0 && !meta.allowWhenFrozen) {
      console.log("[Router] Tenant frozen, redirect to billing");
      return { path: "/billing" };
    }

    // AI 设计助手插件路由有独立的身份守卫，全局守卫不做拦截
    if (!to.path.startsWith('/plugins/ai-design-assistant')) {
      if (!hasRoutePermission(auth.roleCode, meta.roles as string[] | undefined)) {
        console.log("[Router] No permission, redirect to forbidden. roleCode:", auth.roleCode, "roles:", meta.roles);
        return { path: "/forbidden" };
      }
    }

    console.log("[Router] Navigation allowed");
    return true;
  });
}