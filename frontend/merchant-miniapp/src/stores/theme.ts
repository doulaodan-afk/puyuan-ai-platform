import { defineStore } from 'pinia';
import { storage } from '@/utils/storage';
import { CONFIG } from '@/utils/config';

export type ThemeMode = 'light' | 'dark' | 'auto';
type Resolved = 'light' | 'dark';

const STORAGE_KEY = 'puyuan-theme'; // 与 Web 端保持一致

function getSystemPref(): Resolved {
  // 微信小程序通过 Taro.getSystemInfoSync() 获取系统主题
  try {
    const systemInfo = Taro.getSystemInfoSync();
    return systemInfo.theme === 'dark' ? 'dark' : 'light';
  } catch {
    return 'light';
  }
}

function resolveMode(mode: ThemeMode): Resolved {
  return mode === 'auto' ? getSystemPref() : mode;
}

export const useThemeStore = defineStore('theme', {
  state: () => ({
    theme: (storage.get<ThemeMode>(STORAGE_KEY, 'auto') || 'auto') as ThemeMode,
    resolved: 'light' as Resolved
  }),

  getters: {
    isDark(): boolean {
      return this.resolved === 'dark';
    }
  },

  actions: {
    // 初始化主题
    initTheme() {
      this.theme = (storage.get<ThemeMode>(STORAGE_KEY, 'auto') || 'auto') as ThemeMode;
      this.updateResolved();
    },

    // 更新解析后的主题
    updateResolved() {
      this.resolved = resolveMode(this.theme);
    },

    // 设置主题模式
    setTheme(mode: ThemeMode) {
      this.theme = mode;
      this.updateResolved();
      storage.set(STORAGE_KEY, mode);
    },

    // 切换主题
    toggleTheme() {
      const modes: ThemeMode[] = ['light', 'dark', 'auto'];
      const currentIndex = modes.indexOf(this.theme);
      const nextIndex = (currentIndex + 1) % modes.length;
      this.setTheme(modes[nextIndex]);
    },

    // 在 auto 模式下响应系统主题变化
    onSystemThemeChange(dark: boolean) {
      if (this.theme === 'auto') {
        this.resolved = dark ? 'dark' : 'light';
      }
    }
  }
});
