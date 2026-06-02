import type { RouteRecordRaw } from 'vue-router'

const PluginLayout = () => import('./pages/PluginLayout.vue')
const HomePage = () => import('./pages/home.vue')

export const pluginRoutes: RouteRecordRaw[] = [
  {
    path: '/plugins/{{PLUGIN_ID}}',
    component: PluginLayout,
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/plugins/{{PLUGIN_ID}}/home',
      },
      {
        path: 'home',
        name: '{{PLUGIN_ID}}Home',
        component: HomePage,
        meta: { title: '{{PLUGIN_NAME}}', requiresAuth: true },
      },
    ],
  },
]

export default pluginRoutes