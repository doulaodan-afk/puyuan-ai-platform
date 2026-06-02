import { http } from '@/utils/request';

export interface InvokePluginRequest {
  prompt?: string;
  image_size?: string;
  product_desc?: string;
  product_url?: string;
  script_type?: string;
  text?: string;
  target_lang?: string;
}

export interface InvokePluginResponse {
  data?: {
    image_url?: string;
    image_size?: string;
    script?: string;
    script_type?: string;
    translated_text?: string;
    target_lang?: string;
    source_lang?: string;
  };
  token_used: number;
  balance_remaining: number;
}

export interface PluginInfo {
  pluginId: string;
  pluginName: string;
  pluginType: string;
  description: string;
  iconUrl: string;
  status: number;
  pricing: string;
  enabled?: number;
}

export const pluginService = {
  // 调用插件
  async invoke(pluginCode: string, params: InvokePluginRequest): Promise<InvokePluginResponse> {
    return http.post(`/api/plugin/invoke/${pluginCode}`, params, { showLoading: true, showErrorMessage: true });
  },

  // 获取插件列表
  async list(): Promise<PluginInfo[]> {
    return http.get('/api/plugin/list', {}, { showLoading: false });
  },

  // 获取已启用的插件
  async getEnabledPlugins(): Promise<PluginInfo[]> {
    const list = await this.list();
    return list.filter(p => p.status === 1 && p.enabled === 1);
  }
};

// AI 服务类型
export const AI_SERVICES = {
  IMAGE_GEN: 'ai_image_gen',
  SCRIPT_GEN: 'ai_script_gen',
  TRANSLATE: 'ai_translate'
} as const;

// 图片尺寸选项
export const IMAGE_SIZES = [
  { value: '512x512', label: '512×512', tokens: 10 },
  { value: '1024x1024', label: '1024×1024', tokens: 20 },
  { value: '1792x1024', label: '1792×1024 (横)', tokens: 30 },
  { value: '1024x1792', label: '1024×1792 (竖)', tokens: 30 }
] as const;

// 脚本类型选项
export const SCRIPT_TYPES = [
  { value: 'video', label: '短视频' },
  { value: 'live', label: '直播' },
  { value: 'detail', label: '详情页' }
] as const;

// 翻译语言选项
export const TRANSLATE_LANGS = [
  { value: 'en', label: '英语', tokensPer10Chars: 1 },
  { value: 'th', label: '泰语', tokensPer10Chars: 1 },
  { value: 'vi', label: '越南语', tokensPer10Chars: 1 },
  { value: 'ms', label: '马来语', tokensPer10Chars: 1 },
  { value: 'id', label: '印尼语', tokensPer10Chars: 1 }
] as const;

// 计算图片生成 Token 费用
export function calculateImageTokenCost(size: string): number {
  const option = IMAGE_SIZES.find(s => s.value === size);
  return option?.tokens || 20;
}

// 计算翻译 Token 费用
export function calculateTranslateTokenCost(textLength: number): number {
  const cost = Math.ceil(textLength / 10);
  return Math.max(cost, 5);
}
