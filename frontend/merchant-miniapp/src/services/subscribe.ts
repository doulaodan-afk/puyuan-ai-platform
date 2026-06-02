import { http } from '@/utils/request';

export interface MiniCodeResponse {
  code: string;
}

export interface ShareRecordRequest {
  pluginCode: string;
  resultType: 'image' | 'text' | 'script';
  resultData: any;
  resultUrl: string;
  formId: string;
}

export interface RechargeSuccessRequest {
  tokenGrant: number;
  orderNo: string;
}

export const subscribeService = {
  /**
   * 发送余额不足提醒
   */
  async sendBalanceLowNotification(): Promise<string> {
    return http.post('/api/v1/subscribe/balance/low', {}, { showLoading: false });
  },

  /**
   * 发送充值成功通知
   */
  async sendRechargeSuccessNotification(params: RechargeSuccessRequest): Promise<string> {
    return http.post('/api/v1/subscribe/recharge/success', params, { showLoading: false });
  },

  /**
   * 获取小程序码（用于分享）
   */
  async generateMiniappCode(pluginCode: string, page: string = 'index'): Promise<MiniCodeResponse> {
    return http.get('/api/v1/subscribe/minicode', { pluginCode, page }, { showLoading: true });
  },

  /**
   * 记录分享行为
   */
  async recordShare(params: ShareRecordRequest): Promise<string> {
    return http.post('/api/v1/subscribe/share/record', params, { showLoading: false });
  }
};
