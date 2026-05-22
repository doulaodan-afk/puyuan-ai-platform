import Taro from '@tarojs/taro';
import { CONFIG } from './config';
import { useAuthStore } from '@/stores/auth';
import { useThemeStore } from '@/stores/theme';

export interface ApiResponse<T = any> {
  code: number;
  message: string;
  data: T;
}

export interface RequestOptions extends Taro.request.Option {
  showLoading?: boolean;
  showErrorMessage?: boolean;
  auth?: boolean;
}

// 构建 Request Headers
function buildHeaders(extra?: Record<string, string>): Record<string, string> {
  const authStore = useAuthStore();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...extra
  };

  if (authStore.accessToken) {
    headers.Authorization = `Bearer ${authStore.accessToken}`;
  }
  if (authStore.currentTenantId) {
    headers['X-Tenant-Id'] = String(authStore.currentTenantId);
  }
  if (authStore.userId) {
    headers['X-User-Id'] = String(authStore.userId);
  }

  // 添加请求 ID
  headers['X-Request-Id'] = generateRequestId();

  return headers;
}

function generateRequestId(): string {
  return 'req-' + Date.now() + '-' + Math.random().toString(36).substring(2, 15);
}

// 统一请求函数
export async function request<T = any>(
  url: string,
  options: RequestOptions = {}
): Promise<T> {
  const {
    showLoading = true,
    showErrorMessage = true,
    auth = true,
    ...requestOptions
  } = options;

  const fullUrl = url.startsWith('http') ? url : `${CONFIG.API_BASE_URL}${url}`;

  if (showLoading) {
    Taro.showLoading({
      title: '加载中...',
      mask: true
    });
  }

  try {
    const response = await Taro.request({
      url: fullUrl,
      method: requestOptions.method || 'GET',
      data: requestOptions.data,
      header: buildHeaders(requestOptions.header),
      timeout: CONFIG.REQUEST_TIMEOUT,
      ...requestOptions
    });

    if (showLoading) {
      Taro.hideLoading();
    }

    const { statusCode, data } = response;

    if (statusCode === 401) {
      // Token 过期，跳转登录
      const authStore = useAuthStore();
      authStore.clearAuth();
      Taro.reLaunch({
        url: '/pages/login/index'
      });
      throw new Error('登录已过期，请重新登录');
    }

    if (statusCode !== 200) {
      throw new Error(`HTTP ${statusCode}`);
    }

    const payload = data as ApiResponse<T>;

    if (payload.code !== 0 && payload.code !== 200) {
      const errorMsg = payload.message || '请求失败';
      if (showErrorMessage) {
        Taro.showToast({
          title: errorMsg,
          icon: 'none',
          duration: 2000
        });
      }
      throw new Error(errorMsg);
    }

    return payload.data;
  } catch (error) {
    if (showLoading) {
      Taro.hideLoading();
    }

    const message = error instanceof Error ? error.message : '网络错误，请稍后重试';

    if (showErrorMessage) {
      Taro.showToast({
        title: message,
        icon: 'none',
        duration: 2000
      });
    }

    throw error;
  }
}

// 便捷请求方法
export const http = {
  get<T = any>(url: string, data?: any, options?: RequestOptions): Promise<T> {
    return request<T>(url, {
      method: 'GET',
      data,
      ...options
    });
  },

  post<T = any>(url: string, data?: any, options?: RequestOptions): Promise<T> {
    return request<T>(url, {
      method: 'POST',
      data,
      ...options
    });
  },

  put<T = any>(url: string, data?: any, options?: RequestOptions): Promise<T> {
    return request<T>(url, {
      method: 'PUT',
      data,
      ...options
    });
  },

  delete<T = any>(url: string, data?: any, options?: RequestOptions): Promise<T> {
    return request<T>(url, {
      method: 'DELETE',
      data,
      ...options
    });
  }
};

export default http;
