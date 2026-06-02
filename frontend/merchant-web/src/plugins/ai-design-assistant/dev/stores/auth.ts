import { defineStore } from "pinia";

// Stub auth store for standalone plugin development
// Provides the same interface as the real auth store but with mock data

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

const mockMembers: MemberInfo[] = [
  { userId: 1, nickname: "张老板", mobile: "13800000001", role: "boss", status: "active", joinedAt: "2026-01-01T00:00:00Z" },
  { userId: 2, nickname: "李设计师", mobile: "13800000002", role: "designer", status: "active", joinedAt: "2026-02-01T00:00:00Z" },
  { userId: 3, nickname: "王助理", mobile: "13800000003", role: "design_assistant", status: "active", joinedAt: "2026-03-01T00:00:00Z" },
  { userId: 4, nickname: "赵制版师", mobile: "13800000004", role: "pattern_maker", status: "active", joinedAt: "2026-03-15T00:00:00Z" },
];

export const useAuthStore = defineStore("auth", {
  state: () => ({
    accessToken: "mock-token",
    tenantId: "2001",
    profileLoaded: true,
    tenantStatus: 1,
    roleCode: "boss" as string | null,
    userId: 1 as number | null,
  }),
  getters: {
    isBoss: (state) => state.roleCode === "boss",
    isLoggedIn: () => true,
    currentRole: (state) => state.roleCode,
  },
  actions: {
    setAccessToken(_token: string) {},
    setTenantId(_tenantId: string) {},
    async loadProfile() {},
    clearAuth() {},
    async getMembers(): Promise<MemberInfo[]> {
      return mockMembers;
    },
    async inviteMember(mobile: string, role: string): Promise<CommonResponse> {
      mockMembers.push({ userId: mockMembers.length + 1, nickname: mobile, mobile, role, status: "active", joinedAt: new Date().toISOString() });
      return { success: true, message: "邀请成功" };
    },
    async updateMemberRole(userId: number, role: string): Promise<CommonResponse> {
      const m = mockMembers.find(m => m.userId === userId);
      if (m) m.role = role;
      return { success: true, message: "修改成功" };
    },
    async removeMember(userId: number): Promise<CommonResponse> {
      const idx = mockMembers.findIndex(m => m.userId === userId);
      if (idx !== -1) mockMembers.splice(idx, 1);
      return { success: true, message: "移除成功" };
    },
  },
});

export type { MemberInfo, CommonResponse };