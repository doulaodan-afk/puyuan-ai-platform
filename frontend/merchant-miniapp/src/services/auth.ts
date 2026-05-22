import { http } from '@/utils/request';

export interface LoginRequest {
  mobile: string;
  code?: string;
  password?: string;
}

export interface WxLoginRequest {
  code: string;
  userInfo?: {
    nickName?: string;
    avatarUrl?: string;
    gender?: number;
    language?: string;
    city?: string;
    province?: string;
    country?: string;
  };
}

export interface LoginResponse {
  accessToken: string;
  expiresIn: number;
  userId: number;
  mobile: string;
  nickname: string;
  tenants: any[];
  tenantId?: number;
  role?: string;
}

export interface ProfileResponse {
  tenantId: number;
  tenantCode: string;
  tenantName: string;
  tenantStatus: number;
  userId: number;
  role: string;
}

export const authService = {
  // 登录
  async login(data: LoginRequest): Promise<LoginResponse> {
    return http.post('/api/v1/auth/login', data, { showLoading: true });
  },

  // 微信授权登录
  async wxLogin(data: WxLoginRequest): Promise<LoginResponse> {
    return http.post('/api/v1/auth/wx_login', data, { showLoading: true });
  },

  // 发送验证码
  async sendCode(mobile: string): Promise<void> {
    return http.post('/api/v1/auth/send-code', { mobile }, { showLoading: true });
  },

  // 获取个人资料
  async getProfile(): Promise<ProfileResponse> {
    return http.get('/api/v1/tenant/profile', {}, { showLoading: false });
  },

  // 获取成员列表
  async getMembers(): Promise<any[]> {
    return http.get('/api/tenant/members', {}, { showLoading: false });
  },

  // 邀请成员
  async inviteMember(mobile: string, role: string): Promise<{ success: boolean; message: string }> {
    return http.post('/api/tenant/invite', { mobile, role }, { showLoading: true });
  },

  // 更新成员角色
  async updateMemberRole(targetUserId: number, newRole: string): Promise<{ success: boolean; message: string }> {
    return http.put(`/api/tenant/members/${targetUserId}/role`, { role: newRole }, { showLoading: true });
  },

  // 移除成员
  async removeMember(targetUserId: number): Promise<{ success: boolean; message: string }> {
    return http.delete(`/api/tenant/members/${targetUserId}`, {}, { showLoading: true });
  }
};
