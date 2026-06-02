import type { App } from 'vue'
import type { Pinia } from 'pinia'
import type { Router } from 'vue-router'
import { pluginRoutes } from './router'

export function install(app: App, pinia: Pinia, router: Router): void {
  pluginRoutes.forEach(route => {
    router.addRoute(route as any)
  })
  console.log('[Plugin] ai-image-gen installed successfully')
}

export { pluginRoutes }

export default install
