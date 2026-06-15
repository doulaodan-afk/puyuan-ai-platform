import { defineStore } from "pinia";
import { generateUUID } from "../utils/uuid";

interface ProfileResponse {
  tenant_status: number;
  role_code: string;
  tenant_name: string;
  logo_url: string;
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

export interface UserTenantInfo {
  tenantId: number;
  tenantName: string;
  tenantCode: string;
  role: string;
  isDefault: boolean;
}

function buildHeaders(): HeadersInit {
  const headers: Record<string, string> = {
    "X-Request-Id": generateUUID(),
  };
  if (this.accessToken) {
    headers.Authorization = `Bearer ${this.accessToken}`;
  }
  if (this.tenantId) {
    headers["X-Tenant-Id"] = this.tenantId;
  }
  return headers;
}

function parseUserIdFromToken(token: string): string {
  const tokenRaw = token.replace('Bearer ', '');
  const tokenParts = tokenRaw.split('-');
  return tokenParts.length >= 2 ? tokenParts[1] : '';
}

// 角色中文映射
export const ROLE_LABEL_MAP: Record<string, string> = {
  boss: '管理员',
  designer: '设计师',
  design_assistant: '设计助理',
  pattern_maker: '版师',
  operator: '面料特供商',
  viewer: '查看者',
  merchant_owner: '管理员',
  merchant_operator: '面料特供商',
  merchant_viewer: '查看者',
  tenant_admin: '管理员',
  tenant_operator: '面料特供商',
  tenant_viewer: '查看者',
};

export function getRoleLabel(role: string): string {
  return ROLE_LABEL_MAP[role] || role;
}

export const useAuthStore = defineStore("auth", {
  state: () => ({
    accessToken: localStorage.getItem("merchant_access_token") ?? "",
    tenantId: localStorage.getItem("merchant_tenant_id") ?? "",
    profileLoaded: false,
    tenantStatus: 1,
    roleCode: null as string | null,
    userId: null as number | null,
    isSandbox: false,
    tenantName: null as string | null,
    enterpriseName: localStorage.getItem("merchant_enterprise_name") ?? null as string | null,
    logoUrl: null as string | null,
    // 用户的所有工作室列表（从 localStorage 恢复，避免刷新后丢失）
    tenants: JSON.parse(localStorage.getItem("merchant_tenants") ?? "[]") as UserTenantInfo[],
    tenantsLoaded: !!JSON.parse(localStorage.getItem("merchant_tenants") ?? "[]").length,
  }),
  getters: {
    isBoss: (state) => state.roleCode === "boss",
    isLoggedIn: (state) => !!state.accessToken && !!state.tenantId,
    currentRole: (state) => state.roleCode,
    // 是否有多个工作室
    hasMultipleTenants: (state) => state.tenants.length > 1,
    // 当前工作室信息
    currentTenant: (state) => state.tenants.find(t => String(t.tenantId) === state.tenantId) ?? null,
    // 当前工作室ID（数值型，供组件使用）
    currentTenantId: (state) => Number(state.tenantId) || 0,
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
    syncTenantsToStorage() {
      localStorage.setItem("merchant_tenants", JSON.stringify(this.tenants));
    },
    syncDocumentTitle() {
      document.title = this.tenantName ? `濮院毛衫AI平台 + ${this.tenantName}` : '濮院毛衫AI平台';
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

      const payload = (await response.json()) as ProfileApiResponse;
      if (payload.code !== 0 || !payload.data) {
        throw new Error(payload.message || "load profile failed");
      }
      this.tenantStatus = payload.data.tenant_status;
      this.roleCode = payload.data.role_code;
      this.tenantName = payload.data.tenant_name ?? null;
      this.logoUrl = payload.data.logo_url ?? null;
      // 首次加载时初始化企业名称（登录时当前租户即企业租户）
      if (!this.enterpriseName && this.tenantName) {
        this.enterpriseName = this.tenantName;
        localStorage.setItem("merchant_enterprise_name", this.tenantName);
      }
      this.profileLoaded = true;
      this.syncDocumentTitle();
    },
    /**
     * 加载用户的所有工作室列表
     */
    async loadTenants(): Promise<UserTenantInfo[]> {
      if (!this.accessToken) {
        this.tenants = [];
        this.syncTenantsToStorage();
        return [];
      }
      try {
        const userId = parseUserIdFromToken(this.accessToken);
        const response = await fetch('/api/tenant/user/tenants', {
          headers: {
            'Authorization': `Bearer ${this.accessToken}`,
            'X-User-Id': userId,
            'X-Request-Id': generateUUID(),
          },
        });
        if (!response.ok) {
          console.error('加载工作室列表HTTP错误:', response.status);
          this.tenantsLoaded = true;
          return this.tenants;
        }
        const result = await response.json();
        if (result.code === 0 && result.data) {
          this.tenants = result.data.map((s: any) => ({
            tenantId: s.tenantId ?? s.tenant_id,
            tenantName: s.tenantName ?? s.tenant_name,
            tenantCode: s.tenantCode ?? s.tenant_code,
            role: s.role ?? s.role_code,
            isDefault: s.isDefault ?? s.is_default ?? false,
          }));
          this.syncTenantsToStorage();
        } else {
          // API 返回业务错误，保留已有数据而非覆盖为空
          this.tenantsLoaded = true;
          return this.tenants;
        }
        this.tenantsLoaded = true;
      } catch (e) {
        console.error('加载工作室列表失败', e);
        this.tenantsLoaded = true;
        return this.tenants;
      }
      return this.tenants;
    },
    /**
     * 直接设置工作室列表（来自登录接口返回数据）
     */
    setTenants(rawList: any[]) {
      this.tenants = rawList.map((s: any) => ({
        tenantId: s.tenantId ?? s.tenant_id,
        tenantName: s.tenantName ?? s.tenant_name,
        tenantCode: s.tenantCode ?? s.tenant_code,
        role: s.role ?? s.role_code,
        isDefault: s.isDefault ?? s.is_default ?? false,
      }));
      this.tenantsLoaded = true;
      this.syncTenantsToStorage();
    },
    /**
     * 创建新工作室
     */
    async createTenant(tenantName: string): Promise<{ success: boolean; message: string; data?: UserTenantInfo }> {
      try {
        const userId = parseUserIdFromToken(this.accessToken);
        const response = await fetch('/api/tenant/create', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${this.accessToken}`,
            'X-User-Id': userId,
            'X-Request-Id': generateUUID(),
          },
          body: JSON.stringify({ tenantName }),
        });
        if (!response.ok) {
          return { success: false, message: `创建失败 (HTTP ${response.status})` };
        }
        const result = await response.json();
        if (result.code === 0 && result.data) {
          const newTenant: UserTenantInfo = {
            tenantId: result.data.tenantId ?? result.data.tenant_id,
            tenantName: result.data.tenantName ?? result.data.tenant_name ?? tenantName,
            tenantCode: result.data.tenantCode ?? result.data.tenant_code ?? '',
            role: result.data.role ?? 'boss',
            isDefault: false,
          };
          this.tenants.push(newTenant);
          this.syncTenantsToStorage();
          // 异步刷新完整列表
          this.loadTenants().catch(() => {});
          return { success: true, message: '工作室创建成功', data: newTenant };
        }
        return { success: false, message: result.message || '创建失败' };
      } catch (e) {
        console.error('创建工作室失败', e);
        return { success: false, message: '网络错误，创建失败' };
      }
    },
    /**
     * 删除工作室/租户（仅 boss 可操作）
     */
    async deleteTenant(targetTenantId: number): Promise<{ success: boolean; message: string }> {
      try {
        const userId = parseUserIdFromToken(this.accessToken);
        const response = await fetch(`/api/tenant/${targetTenantId}`, {
          method: 'DELETE',
          headers: {
            'Authorization': `Bearer ${this.accessToken}`,
            'X-User-Id': userId,
            'X-Request-Id': generateUUID(),
          },
        });
        const result = await response.json();
        if (result.code === 0) {
          // 从本地列表中移除
          this.tenants = this.tenants.filter(t => t.tenantId !== targetTenantId);
          this.syncTenantsToStorage();
          // 如果删除的是当前工作室，切换到剩余的第一个
          if (String(targetTenantId) === this.tenantId) {
            if (this.tenants.length > 0) {
              await this.switchTenant(this.tenants[0].tenantId);
            } else {
              this.setTenantId('');
              this.tenantName = null;
            }
          }
          return { success: true, message: '工作室已删除' };
        }
        return { success: false, message: result.message || '删除失败' };
      } catch (e) {
        console.error('删除工作室失败', e);
        return { success: false, message: '网络错误，删除失败' };
      }
    },
    /**
     * 切换到指定工作室
     * @param targetTenantId 目标工作室ID
     * @returns 切换结果
     */
    async switchTenant(targetTenantId: number): Promise<{ success: boolean; message: string }> {
      try {
        const userId = parseUserIdFromToken(this.accessToken);
        const response = await fetch('/api/tenant/switch', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${this.accessToken}`,
            'X-User-Id': userId,
            'X-Request-Id': generateUUID(),
          },
          body: JSON.stringify({ tenant_id: targetTenantId }),
        });
        const result = await response.json();
        if (result.code !== 0) {
          return { success: false, message: result.message || '切换失败' };
        }

        const data = result.data;
        // 更新当前租户信息
        this.setTenantId(String(data.tenantId ?? data.tenant_id));
        this.roleCode = data.role ?? data.role_code;
        this.tenantName = data.tenantName ?? data.tenant_name ?? null;
        this.userId = data.userId ?? data.user_id ?? null;
        if (data.accessToken ?? data.access_token) {
          this.setAccessToken(data.accessToken ?? data.access_token);
        }
        this.profileLoaded = false;
        this.syncDocumentTitle();

        // 同步更新 AI 设计助手的 localStorage 标记
        const targetTenant = this.tenants.find(t => t.tenantId === targetTenantId);
        if (targetTenant) {
          const roleLabel = getRoleLabel(targetTenant.role);
          localStorage.setItem('ai_design_tenant_id', String(targetTenantId));
          localStorage.setItem('ai_design_role', targetTenant.role);
          localStorage.setItem('ai_design_identity_prefix', `${targetTenant.tenantName}-${roleLabel}`);
        }

        return { success: true, message: '切换成功' };
      } catch (e) {
        console.error('切换工作室失败', e);
        return { success: false, message: '网络错误，切换失败' };
      }
    },
    clearAuth() {
      this.accessToken = "";
      this.tenantId = "";
      this.profileLoaded = false;
      this.tenantStatus = 1;
      this.roleCode = null;
      this.userId = null;
      this.isSandbox = false;
      this.tenantName = null;
      this.enterpriseName = null;
      this.logoUrl = null;
      this.tenants = [];
      this.tenantsLoaded = false;
      localStorage.removeItem("merchant_access_token");
      localStorage.removeItem("merchant_tenant_id");
      localStorage.removeItem("merchant_enterprise_name");
      localStorage.removeItem("merchant_tenants");
      localStorage.removeItem("ai_design_tenant_id");
      localStorage.removeItem("ai_design_role");
      localStorage.removeItem("ai_design_identity_prefix");
      this.syncDocumentTitle();
    },
    async getMembers(): Promise<MemberInfo[]> {
      const userId = parseUserIdFromToken(this.accessToken);
      const response = await fetch("/api/tenant/members", {
        headers: {
          ...buildHeaders.call(this),
          "X-User-Id": userId,
        },
      });
      const payload = await response.json();
      if (payload.code !== 0) {
        throw new Error(payload.message || "获取成员列表失败");
      }
      // 后端返回 snake_case，转为 camelCase 供前端使用
      const rawList: any[] = payload.data ?? [];
      return rawList
        .filter((item: any) => item.status === 'active') // 只显示活跃成员
        .map((item: any) => ({
          userId: item.user_id ?? item.userId,
          nickname: item.nickname,
          mobile: item.mobile,
          role: item.role ?? item.role_code,
          status: item.status,
          inviterName: item.inviter_name ?? item.inviterName,
          joinedAt: item.joined_at ?? item.joinedAt,
        }));
    },
    async inviteMember(mobile: string, role: string): Promise<CommonResponse> {
      const userId = parseUserIdFromToken(this.accessToken);
      const response = await fetch("/api/tenant/invite", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          ...buildHeaders.call(this),
          "X-User-Id": userId,
        },
        body: JSON.stringify({ mobile, role }),
      });
      const payload = await response.json();
      return {
        success: payload.code === 0,
        message: payload.message || (payload.code === 0 ? "邀请成功" : "邀请失败"),
      };
    },
    async updateMemberRole(memberUserId: number, role: string): Promise<CommonResponse> {
      const userId = parseUserIdFromToken(this.accessToken);
      const response = await fetch(`/api/tenant/members/${memberUserId}/role`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          ...buildHeaders.call(this),
          "X-User-Id": userId,
        },
        body: JSON.stringify({ role_code: role }),
      });
      const payload = await response.json();
      return {
        success: payload.code === 0,
        message: payload.message || (payload.code === 0 ? "修改成功" : "修改失败"),
      };
    },
    async removeMember(memberUserId: number): Promise<CommonResponse> {
      const userId = parseUserIdFromToken(this.accessToken);
      const response = await fetch(`/api/tenant/members/${memberUserId}`, {
        method: "DELETE",
        headers: {
          ...buildHeaders.call(this),
          "X-User-Id": userId,
        },
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