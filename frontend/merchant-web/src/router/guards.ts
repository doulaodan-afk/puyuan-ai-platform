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

  // Map backend roles to frontend roles
  const roleMap: Record<string, string[]> = {
    'boss': ['merchant_owner', 'boss'],
    'tenant_admin': ['merchant_owner', 'tenant_admin'],
    'tenant_operator': ['merchant_operator', 'tenant_operator'],
    'tenant_viewer': ['merchant_viewer', 'tenant_viewer']
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

    if (!hasRoutePermission(auth.roleCode, meta.roles as string[] | undefined)) {
      console.log("[Router] No permission, redirect to forbidden. roleCode:", auth.roleCode, "roles:", meta.roles);
      return { path: "/forbidden" };
    }

    console.log("[Router] Navigation allowed");
    return true;
  });
}