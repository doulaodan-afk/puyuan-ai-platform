import { defineStore } from "pinia";

interface ProfileResponse {
  tenant_status: number;
  role_code: string;
}

interface ProfileApiResponse {
  code: number;
  message: string;
  data: ProfileResponse | null;
}

interface MemberInfo {
  userId: number;
  nickname: string;
  mobile: string;
  role: string;
  status: string;
  inviterName?: string;
  joinedAt?: string;
}

interface CommonResponse {
  success: boolean;
  message: string;
}

function buildHeaders(): HeadersInit {
  const headers: Record<string, string> = {
    "X-Request-Id": crypto.randomUUID(),
  };
  if (this.accessToken) {
    headers.Authorization = `Bearer ${this.accessToken}`;
  }
  if (this.tenantId) {
    headers["X-Tenant-Id"] = this.tenantId;
  }
  return headers;
}

export const useAuthStore = defineStore("auth", {
  state: () => ({
    accessToken: localStorage.getItem("merchant_access_token") ?? "",
    tenantId: localStorage.getItem("merchant_tenant_id") ?? "",
    profileLoaded: false,
    tenantStatus: 1,
    roleCode: null as string | null,
    userId: null as number | null,
  }),
  getters: {
    isBoss: (state) => state.roleCode === "boss",
    isLoggedIn: (state) => !!state.accessToken && !!state.tenantId,
    currentRole: (state) => state.roleCode,
  },
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
      if (payload.code !== 0 || !payload.data) {
        throw new Error(payload.message || "load profile failed");
      }
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
      this.userId = null;
      localStorage.removeItem("merchant_access_token");
      localStorage.removeItem("merchant_tenant_id");
    },
    async getMembers(): Promise<MemberInfo[]> {
      const response = await fetch("/api/tenant/members", {
        headers: buildHeaders.call(this),
      });
      const payload = await response.json();
      if (payload.code !== 0) {
        throw new Error(payload.message || "获取成员列表失败");
      }
      return payload.data ?? [];
    },
    async inviteMember(mobile: string, role: string): Promise<CommonResponse> {
      const response = await fetch("/api/tenant/invite", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          ...buildHeaders.call(this),
        },
        body: JSON.stringify({ mobile, role }),
      });
      const payload = await response.json();
      return {
        success: payload.code === 0,
        message: payload.message || (payload.code === 0 ? "邀请成功" : "邀请失败"),
      };
    },
    async updateMemberRole(userId: number, role: string): Promise<CommonResponse> {
      const response = await fetch(`/api/tenant/members/${userId}/role`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          ...buildHeaders.call(this),
        },
        body: JSON.stringify({ role }),
      });
      const payload = await response.json();
      return {
        success: payload.code === 0,
        message: payload.message || (payload.code === 0 ? "修改成功" : "修改失败"),
      };
    },
    async removeMember(userId: number): Promise<CommonResponse> {
      const response = await fetch(`/api/tenant/members/${userId}`, {
        method: "DELETE",
        headers: buildHeaders.call(this),
      });
      const payload = await response.json();
      return {
        success: payload.code === 0,
        message: payload.message || (payload.code === 0 ? "移除成功" : "移除失败"),
      };
    },
  },
});

export type { MemberInfo, CommonResponse };