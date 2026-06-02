import type { RouteRecordRaw } from 'vue-router'

const PluginLayout = () => import('../pages/PluginLayout.vue')
const HomePage = () => import('../pages/home.vue')

export const pluginRoutes: RouteRecordRaw[] = [
  {
    path: '/plugins/ai-image-gen',
    component: PluginLayout,
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/plugins/ai-image-gen/home',
      },
      {
        path: 'home',
        name: 'AiImageGenHome',
        component: HomePage,
        meta: { title: 'AI 商品图生成', requiresAuth: true },
      },
    ],
  },
]

export default pluginRoutes
