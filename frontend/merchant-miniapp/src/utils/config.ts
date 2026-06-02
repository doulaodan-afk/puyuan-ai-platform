// API 配置
export const CONFIG = {
  // 后端 API 基础地址
  // 开发环境使用本地地址，生产环境请替换为实际地址
  API_BASE_URL: process.env.NODE_ENV === 'production'
    ? 'https://api.puyuan-ai.com'
    : 'http://localhost:8080',

  // 请求超时时间（毫秒）
  REQUEST_TIMEOUT: 30000,

  // Storage keys
  STORAGE_KEYS: {
    ACCESS_TOKEN: 'merchant_access_token',
    USER_ID: 'merchant_user_id',
    USER_MOBILE: 'merchant_user_mobile',
    USER_NICKNAME: 'merchant_user_nickname',
    CURRENT_TENANT_ID: 'merchant_current_tenant_id',
    CURRENT_ROLE: 'merchant_current_role',
    USER_TENANTS: 'merchant_user_tenants',
    THEME: 'theme'
  }
};

export default CONFIG;
