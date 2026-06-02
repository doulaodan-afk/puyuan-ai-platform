import { defineStore } from "pinia";

// Stub auth store for standalone plugin development
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
  },
});