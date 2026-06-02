import { http } from '@/utils/request';

export interface BalanceResponse {
  tokenBalance: number;
  storageUsedGb: number;
  storageFreeQuotaGb: number;
  storageExtraGb: number;
  expireDate?: string;
}

export interface LedgerItem {
  bizNo: string;
  entryType: string;
  direction: string;
  tokenAmount: number;
  cashAmount: number;
  balanceAfter: number;
  pluginId: string;
  occurredAt: string;
}

export interface LedgerPageResponse {
  list: LedgerItem[];
  page: number;
  pageSize: number;
  total: number;
}

export interface RechargePackage {
  id: string;
  name: string;
  amount: number;
  tokenGrant: number;
  bonus?: number;
  recommended?: boolean;
}

export interface CreateRechargeOrderRequest {
  packageId: string;
  amount: number;
  tokenGrant: number;
}

export interface RechargeOrderResponse {
  orderNo: string;
  amount: number;
  tokenGrant: number;
  payStatus: string;
  payQrCodeUrl?: string;
  createdAt: string;
}

export interface RechargeOrder {
  orderNo: string;
  amount: number;
  tokenGrant: number;
  payChannel: string;
  payStatus: string;
  paidAt?: string;
  createdAt: string;
}

export interface RechargeOrderPageResponse {
  list: RechargeOrder[];
  page: number;
  pageSize: number;
  total: number;
}

  // 格式化 Token 数量
  formatToken(amount: number): string {
    return `${amount.toLocaleString()} Tokens`;
  },

  // ===== 微信支付 =====

  // 微信支付预下单
  async wxPrepay(data: WxPrepayRequest): Promise<WxPrepayResponse> {
    return http.post('/api/v1/payment/wx/prepay', data, { showLoading: true });
  },

  // Mock 支付成功（开发环境使用）
  async mockPaymentSuccess(orderNo: string): Promise<void> {
    return http.post('/api/v1/payment/wx/mock/success', { orderNo }, { showLoading: true });
  },

  // 查询订单状态
  async queryOrderStatus(orderNo: string): Promise<{ status: string }> {
    return http.get(`/api/v1/payment/wx/order/${orderNo}/status`, {}, { showLoading: false });
  }
};

// ===== 微信支付相关类型 =====

export interface WxPrepayRequest {
  amount: number;  // 金额（元）
  packageName: string;  // 套餐名称
}

export interface WxPrepayResponse {
  appId: string;
  timeStamp: string;
  nonceStr: string;
  package: string;
  signType: string;
  paySign: string;
  packageId: string;
}

  // 获取消费明细
  async getLedger(params?: {
    page?: number;
    pageSize?: number;
    entryType?: string;
  }): Promise<LedgerPageResponse> {
    return http.get('/api/v1/account/ledger', {
      page: params?.page || 1,
      page_size: params?.pageSize || 20,
      entry_type: params?.entryType
    }, { showLoading: true });
  },

  // 获取充值套餐
  async getPackages(): Promise<RechargePackage[]> {
    // Mock 数据，实际应从后端获取
    return [
      { id: 'pkg_100', name: '体验包', amount: 9.9, tokenGrant: 100 },
      { id: 'pkg_500', name: '标准包', amount: 49, tokenGrant: 500 },
      { id: 'pkg_1000', name: '专业包', amount: 99, tokenGrant: 1000, bonus: 100, recommended: true },
      { id: 'pkg_5000', name: '企业包', amount: 499, tokenGrant: 5000, bonus: 1000 },
      { id: 'pkg_10000', name: '尊享包', amount: 999, tokenGrant: 10000, bonus: 3000 }
    ];
  },

  // 创建充值订单
  async createOrder(data: CreateRechargeOrderRequest): Promise<RechargeOrderResponse> {
    return http.post('/api/v1/account/recharge/orders', data, { showLoading: true });
  },

  // 获取充值订单列表
  async getOrders(params?: {
    page?: number;
    pageSize?: number;
    payStatus?: string;
  }): Promise<RechargeOrderPageResponse> {
    const response = await http.get<{ list: RechargeOrder[]; page: number; page_size: number; total: number }>(
      '/api/v1/account/recharge/orders',
      {
        page: params?.page || 1,
        page_size: params?.pageSize || 20,
        pay_status: params?.payStatus
      },
      { showLoading: true }
    );

    // 兼容不同的响应格式
    return {
      list: response.list || [],
      page: response.page || 1,
      pageSize: response.page_size || 20,
      total: response.total || 0
    };
  },

  // 确认充值订单（扫码支付后调用）
  async confirmOrder(orderNo: string): Promise<any> {
    return http.post(`/api/v1/account/recharge/orders/${orderNo}/confirm`, {}, { showLoading: true });
  },

  // 格式化金额
  formatAmount(amount: number): string {
    return `¥${amount.toFixed(2)}`;
  },

  // 格式化 Token 数量
  formatToken(amount: number): string {
    return `${amount.toLocaleString()} Tokens`;
  }
};
