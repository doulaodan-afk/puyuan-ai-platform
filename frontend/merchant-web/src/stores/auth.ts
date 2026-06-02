import { defineStore } from "pinia";

interface ProfileResponse {
  tenant_status: number;
  role_code: string;
}

interface ProfileApiResponse {
  data: ProfileResponse;
}

export const useAuthStore = defineStore("auth", {
  state: () => ({
    accessToken: localStorage.getItem("merchant_access_token") ?? "",
    tenantId: localStorage.getItem("merchant_tenant_id") ?? "",
    profileLoaded: false,
    tenantStatus: 1,
    roleCode: null as string | null,
  }),
  actions: {
    setAccessToken(token: string) {
      this.accessToken = token;
      localStorage.setItem("merchant_access_token", token);
    },
    setTenantId(tenantId: string) {
      this.tenantId = tenantId;
      localStorage.setItem("merchant_tenant_id", tenantId);
    },
    async loadProfile() {
      if (!this.accessToken || !this.tenantId) {
        throw new Error("auth context missing");
      }

      const response = await fetch("/api/v1/tenant/profile", {
        method: "GET",
        headers: {
          "X-Tenant-Id": this.tenantId,
          "X-Request-Id": crypto.randomUUID(),
          Authorization: `Bearer ${this.accessToken}`,
        },
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const payload = (await response.json()) as ProfileApiResponse;
      this.tenantStatus = payload.data.tenant_status;
      this.roleCode = payload.data.role_code;
      this.profileLoaded = true;
    },
    clearAuth() {
      this.accessToken = "";
      this.tenantId = "";
      this.profileLoaded = false;
      this.tenantStatus = 1;
      this.roleCode = null;
      localStorage.removeItem("merchant_access_token");
      localStorage.removeItem("merchant_tenant_id");
    },
  },
});