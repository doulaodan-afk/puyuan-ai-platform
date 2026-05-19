import type { Router } from "vue-router";
import type { AdminRouteMeta } from "./routes";
import { useAdminAuthStore } from "../stores/adminAuth";

function hasRoutePermission(roleCode: string | null, roles?: string[]): boolean {
  if (!roles || roles.length === 0) {
    return true;
  }
  if (!roleCode) {
    return false;
  }
  return roles.includes(roleCode);
}

export function setupAdminRouterGuards(router: Router): void {
  router.beforeEach(async (to) => {
    const auth = useAdminAuthStore();
    const meta = (to.meta ?? {}) as Partial<AdminRouteMeta>;

    if (!meta.requiresAuth) {
      return true;
    }

    if (!auth.accessToken) {
      return { path: "/admin/login", query: { redirect: to.fullPath } };
    }

    if (!auth.profileLoaded) {
      try {
        await auth.loadProfile();
      } catch {
        auth.clearAuth();
        return { path: "/admin/login", query: { redirect: to.fullPath } };
      }
    }

    if (!hasRoutePermission(auth.roleCode, meta.roles)) {
      return { path: "/admin/forbidden" };
    }

    return true;
  });
}
