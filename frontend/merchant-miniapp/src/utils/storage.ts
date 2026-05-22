import Taro from '@tarojs/taro';
import { CONFIG } from './config';

export const storage = {
  // 设置
  set(key: string, value: any): void {
    try {
      const data = typeof value === 'string' ? value : JSON.stringify(value);
      Taro.setStorageSync(key, data);
    } catch (error) {
      console.error('Storage set error:', error);
    }
  },

  // 获取
  get<T = any>(key: string, defaultValue?: T): T | null {
    try {
      const data = Taro.getStorageSync(key);
      if (data === '' || data === null || data === undefined) {
        return defaultValue ?? null;
      }
      // 尝试解析 JSON
      try {
        return JSON.parse(data);
      } catch {
        return data as T;
      }
    } catch (error) {
      console.error('Storage get error:', error);
      return defaultValue ?? null;
    }
  },

  // 删除
  remove(key: string): void {
    try {
      Taro.removeStorageSync(key);
    } catch (error) {
      console.error('Storage remove error:', error);
    }
  },

  // 清空
  clear(): void {
    try {
      Taro.clearStorageSync();
    } catch (error) {
      console.error('Storage clear error:', error);
    }
  },

  // 获取所有 keys
  keys(): string[] {
    try {
      return Taro.getStorageInfoSync().keys;
    } catch (error) {
      console.error('Storage keys error:', error);
      return [];
    }
  }
};

export default storage;
