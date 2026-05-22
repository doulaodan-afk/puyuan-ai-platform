import { computed, watch } from 'vue'
import { useAuthStore } from '../../stores/auth'

export interface MenuItem {
  path: string
  name: string
  icon: string
  roles: string[]
  badge?: number | (() => number)
}

// 定义设计助手插件内的所有菜单项
const pluginMenus: MenuItem[] = [
  {
    path: '/design-assistant/create',
    name: '创建设计需求',
    icon: '✨',
    roles: ['boss', 'designer', 'design_assistant', 'operator'],
  },
  {
    path: '/design-assistant/list',
    name: '我的设计需求',
    icon: '📋',
    roles: ['boss', 'designer', 'design_assistant', 'operator', 'viewer'],
  },
  {
    path: '/design-assistant/pending',
    name: '设计助理待办',
    icon: '🔔',
    roles: ['boss', 'design_assistant'],
  },
  {
    path: '/design-assistant/tasks',
    name: '我的任务',
    icon: '✅',
    roles: ['boss', 'designer', 'pattern_maker', 'operator', 'viewer'],
  },
  {
    path: '/design-assistant/messages',
    name: '消息中心',
    icon: '💬',
    roles: ['boss', 'designer', 'design_assistant', 'pattern_maker', 'operator', 'viewer'],
  },
  {
    path: '/design-assistant/task-board',
    name: '任务看板',
    icon: '📊',
    roles: ['boss', 'designer', 'design_assistant', 'operator', 'viewer'],
  },
  {
    path: '/design-assistant/fabrics',
    name: '面料库管理',
    icon: '🧵',
    roles: ['boss', 'operator'],
  },
  // 成员管理（仅老板）
  {
    path: '/design-assistant/settings',
    name: '成员管理',
    icon: '👥',
    roles: ['boss'],
  },
  // 合作方管理（仅老板）
  {
    path: '/design-assistant/partners',
    name: '合作方管理',
    icon: '🤝',
    roles: ['boss'],
  },
]

export function useMenuFilter() {
  const auth = useAuthStore()

  // 根据当前角色过滤菜单
  const visibleMenus = computed(() => {
    const currentRole = auth.currentRole
    console.log('[MenuFilter] currentRole:', currentRole, 'profileLoaded:', auth.profileLoaded)

    // 如果角色还没加载，返回所有菜单（让路由守卫处理权限）
    if (!currentRole) {
      console.log('[MenuFilter] No role loaded, returning all menus')
      return pluginMenus
    }

    const filtered = pluginMenus.filter((menu) => menu.roles.includes(currentRole))
    console.log('[MenuFilter] Visible menus:', filtered.map(m => m.name))
    return filtered
  })

  // 检查用户是否有访问某个菜单的权限
  function hasMenuAccess(path: string): boolean {
    const menu = pluginMenus.find((m) => m.path === path)
    if (!menu) {
      return false
    }

    return menu.roles.includes(auth.currentRole)
  }

  return {
    pluginMenus,
    visibleMenus,
    hasMenuAccess,
  }
}