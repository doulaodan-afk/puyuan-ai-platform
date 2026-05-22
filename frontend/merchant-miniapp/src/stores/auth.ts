import { defineStore } from 'pinia';
import { storage } from '@/utils/storage';
import { CONFIG } from '@/utils/config';

export interface UserTenant {
  tenantId: number;
  tenantName: string;
  tenantCode: string;
  role: string;
  isDefault: boolean;
}

export interface LoginResponse {
  accessToken: string;
  expiresIn: number;
  userId: number;
  mobile: string;
  nickname: string;
  tenants: UserTenant[];
  tenantId?: number;
  role?: string;
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: storage.get<string>(CONFIG.STORAGE_KEYS.ACCESS_TOKEN, ''),
    userId: storage.get<number>(CONFIG.STORAGE_KEYS.USER_ID, 0),
    mobile: storage.get<string>(CONFIG.STORAGE_KEYS.USER_MOBILE, ''),
    nickname: storage.get<string>(CONFIG.STORAGE_KEYS.USER_NICKNAME, ''),
    currentTenantId: storage.get<number>(CONFIG.STORAGE_KEYS.CURRENT_TENANT_ID, 0),
    currentRole: storage.get<string>(CONFIG.STORAGE_KEYS.CURRENT_ROLE, ''),
    tenants: storage.get<UserTenant[]>(CONFIG.STORAGE_KEYS.USER_TENANTS, []),
    isLoggedIn: false,
    profileLoaded: false
  }),

  getters: {
    isAuth(): boolean {
      return !!(this.accessToken && this.userId > 0);
    },
    currentTenant(): UserTenant | null {
      return this.tenants.find(t => t.tenantId === this.currentTenantId) || null;
    },
    isBoss(): boolean {
      return this.currentRole === 'boss' || this.currentRole === 'merchant_owner';
    },
    canAccessTools(): boolean {
      return ['boss', 'merchant_owner', 'operator', 'merchant_operator', 'designer', 'merchant_editor'].includes(this.currentRole);
    }
  },

  actions: {
    setAuthData(data: LoginResponse) {
      this.accessToken = data.accessToken;
      this.userId = data.userId;
      this.mobile = data.mobile;
      this.nickname = data.nickname;
      this.tenants = data.tenants || [];

      // 持久化到 storage
      storage.set(CONFIG.STORAGE_KEYS.ACCESS_TOKEN, data.accessToken);
      storage.set(CONFIG.STORAGE_KEYS.USER_ID, data.userId);
      storage.set(CONFIG.STORAGE_KEYS.USER_MOBILE, data.mobile);
      storage.set(CONFIG.STORAGE_KEYS.USER_NICKNAME, data.nickname);
      storage.set(CONFIG.STORAGE_KEYS.USER_TENANTS, this.tenants);

      // 设置租户信息（新接口直接返回）
      if (data.tenantId && data.role) {
        this.currentTenantId = data.tenantId;
        this.currentRole = data.role;
      } else if (this.tenants.length > 0) {
        // 使用第一个租户
        this.switchTenant(this.tenants[0].tenantId);
      }

      this.isLoggedIn = true;
      this.profileLoaded = true;
    },

    switchTenant(tenantId: number) {
      const tenant = this.tenants.find(t => t.tenantId === tenantId);
      if (!tenant) {
        throw new Error('租户不存在');
      }

      this.currentTenantId = tenantId;
      this.currentRole = tenant.role;

      storage.set(CONFIG.STORAGE_KEYS.CURRENT_TENANT_ID, tenantId);
      storage.set(CONFIG.STORAGE_KEYS.CURRENT_ROLE, tenant.role);

      console.log(`切换到工作室: ${tenant.tenantName}, 角色: ${tenant.role}`);
    },

    setAccessToken(token: string) {
      this.accessToken = token;
      storage.set(CONFIG.STORAGE_KEYS.ACCESS_TOKEN, token);
    },

    setTenantId(tenantId: number) {
      this.currentTenantId = tenantId;
      storage.set(CONFIG.STORAGE_KEYS.CURRENT_TENANT_ID, tenantId);
    },

    loadFromStorage() {
      this.accessToken = storage.get<string>(CONFIG.STORAGE_KEYS.ACCESS_TOKEN, '');
      this.userId = storage.get<number>(CONFIG.STORAGE_KEYS.USER_ID, 0);
      this.mobile = storage.get<string>(CONFIG.STORAGE_KEYS.USER_MOBILE, '');
      this.nickname = storage.get<string>(CONFIG.STORAGE_KEYS.USER_NICKNAME, '');
      this.currentTenantId = storage.get<number>(CONFIG.STORAGE_KEYS.CURRENT_TENANT_ID, 0);
      this.currentRole = storage.get<string>(CONFIG.STORAGE_KEYS.CURRENT_ROLE, '');
      this.tenants = storage.get<UserTenant[]>(CONFIG.STORAGE_KEYS.USER_TENANTS, []);
      this.isLoggedIn = !!(this.accessToken && this.userId > 0);
    },

    clearAuth() {
      this.accessToken = '';
      this.userId = 0;
      this.mobile = '';
      this.nickname = '';
      this.currentTenantId = 0;
      this.currentRole = '';
      this.tenants = [];
      this.isLoggedIn = false;
      this.profileLoaded = false;

      // 清除 storage
      storage.remove(CONFIG.STORAGE_KEYS.ACCESS_TOKEN);
      storage.remove(CONFIG.STORAGE_KEYS.USER_ID);
      storage.remove(CONFIG.STORAGE_KEYS.USER_MOBILE);
      storage.remove(CONFIG.STORAGE_KEYS.USER_NICKNAME);
      storage.remove(CONFIG.STORAGE_KEYS.CURRENT_TENANT_ID);
      storage.remove(CONFIG.STORAGE_KEYS.CURRENT_ROLE);
      storage.remove(CONFIG.STORAGE_KEYS.USER_TENANTS);
    }
  }
});
