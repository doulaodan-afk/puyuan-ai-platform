import { useAuthStore } from '../stores/auth'
import { useRouter } from 'vue-router'

/**
 * 老板首次进入插件时的引导逻辑
 * 若当前用户是 boss 且租户成员数只有自己，则自动跳转到团队设置
 */
export function useBossFirstVisitGuide() {
  const auth = useAuthStore()
  const router = useRouter()

  async function checkFirstVisit() {
    // 只对 boss 角色执行检查
    if (!auth.isBoss) {
      return
    }

    try {
      const members = await auth.getMembers()
      const memberCount = members.length

      // 如果只有老板一个人，自动跳转到团队设置
      if (memberCount === 1) {
        router.push('/design-assistant/settings')
      }
    } catch (error) {
      console.error('检查成员列表失败', error)
      // 出错时不阻止用户使用，静默失败
    }
  }

  return {
    checkFirstVisit,
  }
}