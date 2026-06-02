import type { Router } from "vue-router";
import type { AppRouteMeta } from "./routes";
import { useAuthStore } from "../stores/auth";

function hasRoutePermission(roleCode: string | null, roles?: string[]): boolean {
  if (!roles || roles.length === 0) {
    return true;
  }
  if (!roleCode) {
    return false;
  }
  return roles.includes(roleCode);
}

export function setupRouterGuards(router: Router): void {
  router.beforeEach(async (to) => {
    const auth = useAuthStore();
    const meta = (to.meta ?? {}) as Partial<AppRouteMeta>;

    if (!meta.requiresAuth) {
      return true;
    }

    if (!auth.accessToken) {
      return { path: "/login", query: { redirect: to.fullPath } };
    }

    if (!auth.profileLoaded) {
      try {
        await auth.loadProfile();
      } catch {
        auth.clearAuth();
        return { path: "/login", query: { redirect: to.fullPath } };
      }
    }

    if (auth.tenantStatus === 0 && !meta.allowWhenFrozen) {
      return { path: "/billing" };
    }

    if (!hasRoutePermission(auth.roleCode, meta.roles)) {
      return { path: "/forbidden" };
    }

    return true;
  });
}