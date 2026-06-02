import { http } from '@/utils/request';

export interface BillItem {
  billNo: string;
  tenantId: number;
  amount: number;
  status: string;
  period: string;
  createdAt: string;
  dueDate?: string;
  paidAt?: string;
}

export interface BillPageResponse {
  list: BillItem[];
  page: number;
  pageSize: number;
  total: number;
  unpaidAmount: number;
}

export const billingService = {
  // 获取账单列表
  async list(params?: {
    page?: number;
    pageSize?: number;
    status?: string;
  }): Promise<BillPageResponse> {
    return http.get('/api/v1/billing/bills', {
      page: params?.page || 1,
      page_size: params?.pageSize || 20,
      status: params?.status
    }, { showLoading: true });
  },

  // 获取账单详情
  async getDetail(billNo: string): Promise<BillItem> {
    return http.get(`/api/v1/billing/bills/${billNo}`, {}, { showLoading: true });
  },

  // 格式化账单状态
  formatStatus(status: string): string {
    const statusMap: Record<string, string> = {
      pending: '待支付',
      paid: '已支付',
      overdue: '已逾期',
      cancelled: '已取消'
    };
    return statusMap[status] || status;
  },

  // 格式化账单周期
  formatPeriod(period: string): string {
    // period 格式: 2024-01 或 2024-Q1
    return period;
  }
};
