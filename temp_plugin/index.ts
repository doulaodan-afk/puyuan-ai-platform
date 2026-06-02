// AI 设计助手插件入口
// 用于将插件注册到主框架

import type { App } from 'vue'
import type { Pinia } from 'pinia'
import type { Router } from 'vue-router'
import { pluginRoutes } from './router'
import { useDesignAssistantStore } from './stores'

// 插件配置
export interface PluginConfig {
  // API 前缀，默认 /api/plugins/ai-design-assistant
  apiPrefix?: string
  // 是否启用插件，默认 true
  enabled?: boolean
}

// 默认配置
const defaultConfig: PluginConfig = {
  enabled: true,
}

// 安装插件
export function install(
  app: App,
  pinia: Pinia,
  router: Router,
  config: PluginConfig = {}
): void {
  const finalConfig = { ...defaultConfig, ...config }

  // 检查是否启用
  if (finalConfig.enabled === false) {
    console.log('[AI Design Assistant] Plugin is disabled')
    return
  }

  // 注册 Store
  const _store = useDesignAssistantStore(pinia)

  // 注册路由 - 直接添加，不依赖父路由
  pluginRoutes.forEach(route => {
    router.addRoute(route as any)
  })

  console.log('[AI Design Assistant] Plugin installed successfully')
}

// 导出插件信息
export const pluginInfo = {
  name: 'ai-design-assistant',
  version: '1.0.0',
  description: 'AI 设计助手插件',
}

// 导出路由
export { pluginRoutes }

// 导出 Store
export { useDesignAssistantStore }

// 默认导出 install 函数
export default install