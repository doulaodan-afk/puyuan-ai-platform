import { defineStore } from "pinia";
import { generateUUID } from "../utils/uuid";

interface LoginData {
  access_token: string;
  tenant_id: number;
  role_code: string;
}

interface ProfileData {
  tenant_status: number;
  role_code: string;
}

interface ApiEnvelope<T> {
  code: number;
  message: string;
  data: T;
}

export const useAdminAuthStore = defineStore("admin-auth", {
  state: () => ({
    accessToken: localStorage.getItem("admin_access_token") ?? "",
    tenantId: localStorage.getItem("admin_tenant_id") ?? "",
    profileLoaded: false,
    tenantStatus: 1,
    roleCode: localStorage.getItem("admin_role_code") || null,
  }),
  actions: {
    setAuthContext(token: string, tenantId: string, roleCode: string) {
      this.accessToken = token;
      this.tenantId = tenantId;
      this.roleCode = roleCode;
      localStorage.setItem("admin_access_token", token);
      localStorage.setItem("admin_tenant_id", tenantId);
      localStorage.setItem("admin_role_code", roleCode);
    },
    clearAuth() {
      this.accessToken = "";
      this.tenantId = "";
      this.profileLoaded = false;
      this.tenantStatus = 1;
      this.roleCode = null;
      localStorage.removeItem("admin_access_token");
      localStorage.removeItem("admin_tenant_id");
      localStorage.removeItem("admin_role_code");
    },
    async login(mobile: string, verifyCode: string) {
      const response = await fetch("/api/v1/auth/login", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-Request-Id": generateUUID(),
        },
        body: JSON.stringify({
          mobile,
          verify_code: verifyCode,
        }),
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const payload = (await response.json()) as ApiEnvelope<LoginData>;
      if (payload.code !== 0) {
        throw new Error(payload.message || "login failed");
      }

      this.setAuthContext(payload.data.access_token, String(payload.data.tenant_id), payload.data.role_code);
      this.profileLoaded = false;
      await this.loadProfile();
    },
    async loadProfile() {
      if (!this.accessToken || !this.tenantId) {
        throw new Error("auth context missing");
      }

      const response = await fetch("/api/v1/tenant/profile", {
        method: "GET",
        headers: {
          "X-Tenant-Id": this.tenantId,
          "X-Request-Id": generateUUID(),
          Authorization: `Bearer ${this.accessToken}`,
        },
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const payload = (await response.json()) as ApiEnvelope<ProfileData>;
      if (payload.code !== 0) {
        throw new Error(payload.message || "load profile failed");
      }

      this.tenantStatus = payload.data.tenant_status;
      this.roleCode = payload.data.role_code;
      localStorage.setItem("admin_role_code", this.roleCode ?? "");
      this.profileLoaded = true;
    },
  },
});