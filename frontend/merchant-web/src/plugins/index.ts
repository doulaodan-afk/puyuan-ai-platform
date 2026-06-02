// 插件注册中心
// 统一管理所有插件的安装

import type { App } from 'vue'
import type { Pinia } from 'pinia'
import type { Router } from 'vue-router'

// 插件接口
export interface Plugin {
  name: string
  install: (app: App, pinia: Pinia, router: Router) => void
}

// 插件列表
const plugins: Plugin[] = []

// 注册插件
export function registerPlugin(plugin: Plugin) {
  plugins.push(plugin)
}

// 安装所有插件
export function installPlugins(app: App, pinia: Pinia, router: Router) {
  plugins.forEach(plugin => {
    try {
      plugin.install(app, pinia, router)
      console.log(`[Plugin] ${plugin.name} installed successfully`)
    } catch (error) {
      console.error(`[Plugin] ${plugin.name} installation failed:`, error)
    }
  })
}

// 动态导入并注册插件
// 使用 /src/plugins/ 路径而非 @/ 别名，因为 @vite-ignore 下浏览器无法解析 @ 别名
export async function loadPlugin(
  app: App,
  pinia: Pinia,
  router: Router,
  pluginName: string,
  pluginPath: string
) {
  try {
    // 将 @/ 别名路径转换为 /src/ 绝对路径，确保浏览器端动态 import 可解析
    const resolvedPath = pluginPath.replace('@/', '/src/')
    const module = await import(/* @vite-ignore */ resolvedPath)
    if (module.default && typeof module.default.install === 'function') {
      const plugin = module.default as Plugin
      plugin.install(app, pinia, router)
      registerPlugin(plugin)
      console.log(`[Plugin] ${pluginName} loaded successfully`)
    } else if (typeof module.install === 'function') {
      const plugin = module as Plugin
      plugin.install(app, pinia, router)
      registerPlugin(plugin)
      console.log(`[Plugin] ${pluginName} loaded successfully`)
    }
  } catch (error) {
    console.error(`[Plugin] ${pluginName} load failed:`, error)
  }
}

// 导出默认安装函数
export default installPlugins