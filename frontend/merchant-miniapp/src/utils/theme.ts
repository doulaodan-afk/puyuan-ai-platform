/**
 * 小程序端主题管理工具
 * 使用类名控制主题：dark-theme / light-theme
 */

export type ThemeMode = 'light' | 'dark' | 'auto'
type Resolved = 'light' | 'dark'

const STORAGE_KEY = 'miniapp-puyuan-theme'

let currentMode: ThemeMode = 'dark'
let resolvedTheme: Resolved = 'dark'

function getSystemPref(): Resolved {
  const systemDark = wx.getSystemInfoSync().theme === 'dark'
  return systemDark ? 'dark' : 'light'
}

function resolveMode(mode: ThemeMode): Resolved {
  return mode === 'auto' ? getSystemPref() : mode
}

/**
 * 获取当前主题模式
 */
export function getTheme(): ThemeMode {
  return currentMode
}

/**
 * 获取解析后的主题（实际应用的主题）
 */
export function getResolvedTheme(): Resolved {
  return resolvedTheme
}

/**
 * 设置主题模式
 */
export function setTheme(mode: ThemeMode) {
  currentMode = mode
  resolvedTheme = resolveMode(mode)

  // 持久化到本地存储
  wx.setStorageSync(STORAGE_KEY, mode)

  // 应用主题类名到页面容器
  applyTheme(resolvedTheme)
}

/**
 * 初始化主题
 */
export function initTheme() {
  try {
    // 读取存储的主题偏好
    const saved = wx.getStorageSync(STORAGE_KEY) as ThemeMode | undefined
    if (saved && ['light', 'dark', 'auto'].includes(saved)) {
      currentMode = saved
    }

    resolvedTheme = resolveMode(currentMode)
    applyTheme(resolvedTheme)
  } catch (e) {
    console.error('初始化主题失败:', e)
  }
}

/**
 * 应用主题到页面
 */
function applyTheme(theme: Resolved) {
  // 获取当前页面实例
  const pages = getCurrentPages()
  if (pages.length > 0) {
    const currentPage = pages[pages.length - 1]
    const pageContainer = currentPage.$scope || currentPage

    // 移除旧的类名，添加新的类名
    pageContainer.setData({
      themeClass: theme === 'dark' ? 'dark-theme' : 'light-theme'
    })
  }
}

/**
 * 监听系统主题变化（小程序不支持，但预留接口）
 */
export function listenSystemThemeChange() {
  // 小程序暂不支持系统主题变化监听
  // 可通过 onThemeChange 生命周期函数监听
}

/**
 * 切换主题（带简单的淡入淡出动画）
 */
export function toggleTheme() {
  const nextMode: ThemeMode = resolvedTheme === 'dark' ? 'light' : 'dark'
  setTheme(nextMode)
  return nextMode
}

/**
 * 获取主题颜色值（供样式使用）
 */
export function getThemeColors() {
  const isDark = resolvedTheme === 'dark'

  return {
    background: isDark ? '#0a0a0a' : '#ffffff',
    foreground: isDark ? '#fafafa' : '#161616',
    card: isDark ? '#121212' : '#ffffff',
    border: isDark ? '#262626' : '#e5e5e5',
    muted: isDark ? '#1e1e1e' : '#f5f5f5',
    mutedForeground: isDark ? '#a3a3a3' : '#737373',
    primary: isDark ? '#fafafa' : '#161616',
    accent: isDark ? '#262626' : '#ededed',
  }
}