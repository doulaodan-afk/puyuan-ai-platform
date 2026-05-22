import { computed } from 'vue'
import { useAuthStore } from '../stores/auth'

export interface MenuItem {
  path: string
  name: string
  icon: string
  roles: string[]
  badge?: number
}

export function useMenuFilter() {
  const userStore = useAuthStore()

  // 定义所有菜单项
  const allMenus: MenuItem[] = [
    // AI 工具
    {
      path: '/ai-tools/image-gen',
      name: 'AI 图片生成',
      icon: '🖼️',
      roles: ['boss', 'designer', 'design_assistant', 'operator'],
    },
    {
      path: '/ai-tools/script-gen',
      name: 'AI 脚本生成',
      icon: '📝',
      roles: ['boss', 'designer', 'design_assistant', 'operator'],
    },
    {
      path: '/ai-tools/translate',
      name: 'AI 跨境翻译',
      icon: '🌐',
      roles: ['boss', 'designer', 'design_assistant', 'operator'],
    },
    // AI 设计助手
    {
      path: '/design-requirement/create',
      name: '创建设计需求',
      icon: '✨',
      roles: ['boss', 'designer', 'design_assistant', 'operator'],
    },
    {
      path: '/design-requirement/list',
      name: '我的设计需求',
      icon: '📋',
      roles: ['boss', 'designer', 'design_assistant', 'operator', 'viewer'],
    },
    {
      path: '/assistant/pending',
      name: '设计助理待办',
      icon: '🔔',
      roles: ['boss', 'design_assistant'],
      badge: computed(() => 0), // TODO: 从 store 获取
    },
    {
      path: '/assistant/detail/:id',
      name: '需求复核',
      icon: '📝',
      roles: ['boss', 'design_assistant'],
    },
    {
      path: '/my-tasks',
      name: '我的任务',
      icon: '✅',
      roles: ['boss', 'designer', 'pattern_maker', 'operator', 'viewer'],
    },
    {
      path: '/messages',
      name: '消息中心',
      icon: '💬',
      roles: ['boss', 'designer', 'design_assistant', 'pattern_maker', 'operator', 'viewer'],
      badge: computed(() => 0), // TODO: 从 store 获取
    },
    {
      path: '/task-board',
      name: '任务看板',
      icon: '📊',
      roles: ['boss', 'designer', 'design_assistant', 'operator', 'viewer'],
    },
    {
      path: '/fabric-manage',
      name: '面料库管理',
      icon: '🧵',
      roles: ['boss', 'operator'],
    },
    // 团队管理（仅老板）
    {
      path: '/design-assistant/settings',
      name: '团队设置',
      icon: '👥',
      roles: ['boss'],
    },
    // 账户
    {
      path: '/account/balance',
      name: '账户余额',
      icon: '💰',
      roles: ['boss', 'operator', 'viewer'],
    },
    {
      path: '/account/recharge',
      name: '充值中心',
      icon: '💳',
      roles: ['boss', 'operator'],
    },
    {
      path: '/account/ledger',
      name: '消费明细',
      icon: '📜',
      roles: ['boss', 'operator', 'viewer'],
    },
    // 账单
    {
      path: '/billing',
      name: '账单中心',
      icon: '🧾',
      roles: ['boss', 'operator', 'viewer'],
    },
    // 设置
    {
      path: '/settings',
      name: '设置',
      icon: '⚙️',
      roles: ['boss'],
    },
  ]

  // 根据当前角色过滤菜单
  const visibleMenus = computed(() => {
    const currentRole = userStore.currentRole

    if (!currentRole) {
      return []
    }

    return allMenus.filter((menu) => {
      return menu.roles.includes(currentRole)
    })
  })

  // 检查用户是否有访问某个菜单的权限
  function hasMenuAccess(path: string): boolean {
    const menu = allMenus.find((m) => m.path === path)
    if (!menu) {
      return false
    }

    return menu.roles.includes(userStore.currentRole)
  }

  return {
    allMenus,
    visibleMenus,
    hasMenuAccess,
  }
}