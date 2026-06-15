<template>
  <div class="guide-page">
    <div class="guide-card">
      <div class="guide-icon">{{ guide.icon }}</div>
      <h2 class="guide-title">{{ guide.title }}</h2>
      <p class="guide-desc">{{ guide.desc }}</p>

      <div class="guide-roles">
        <span class="guide-roles-label">需要以下角色：</span>
        <span v-for="role in guide.targetRoles" :key="role" class="role-tag">{{ getRoleLabel(role) }}</span>
      </div>

      <div class="guide-actions">
        <button class="btn-primary" @click="goInvite">
          <span>📨</span> 邀请成员加入
        </button>
        <button class="btn-secondary" @click="goBack">
          <span>←</span> 返回
        </button>
      </div>

      <div class="guide-hint">
        <p>💡 提示：邀请对应角色的成员加入工作室后，即可使用此功能。</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

// 角色中文映射
const ROLE_LABEL_MAP: Record<string, string> = {
  boss: '管理员',
  designer: '设计师',
  design_assistant: '设计助理',
  pattern_maker: '版师',
  operator: '面料特供商',
  viewer: '查看者',
  merchant_owner: '管理员',
  merchant_operator: '面料特供商',
  merchant_viewer: '查看者',
  tenant_admin: '管理员',
  tenant_operator: '面料特供商',
  tenant_viewer: '查看者',
}

function getRoleLabel(role: string): string {
  return ROLE_LABEL_MAP[role] || role
}

// 引导配置表：根据目标路径决定展示内容
interface GuideConfig {
  icon: string
  title: string
  desc: string
  targetRoles: string[]
}

const GUIDE_MAP: Record<string, GuideConfig> = {
  create: {
    icon: '✨',
    title: '创建设计需求',
    desc: '此功能需要设计师角色才能使用。设计师可以通过 AI 对话快速创建结构化的设计需求，上传参考图片和视频，生成专业的需求文档。',
    targetRoles: ['designer'],
  },
  pending: {
    icon: '🔔',
    title: '设计助理待办',
    desc: '此功能需要设计助理角色才能使用。设计助理可以查看和分配待处理的设计需求，协调设计师和版师之间的工作流程。',
    targetRoles: ['design_assistant'],
  },
  tasks: {
    icon: '✅',
    title: '我的任务',
    desc: '此功能需要设计师、版师、面料特供商或查看者角色才能使用。团队成员可以在此查看和处理分配给自己的任务。',
    targetRoles: ['designer', 'pattern_maker', 'operator', 'viewer'],
  },
  fabrics: {
    icon: '🧵',
    title: '面料库管理',
    desc: '此功能需要面料特供商角色才能使用。面料特供商是与工作室深度合作、开发共创的特别供应商，可以管理工作室的面料库，包括新增、编辑、上架下架面料信息。',
    targetRoles: ['operator'],
  },
  list: {
    icon: '📋',
    title: '我的设计需求',
    desc: '此功能仅限设计师角色使用。设计师可以查看和管理自己创建的所有设计需求，跟踪需求状态并进行后续操作。',
    targetRoles: ['designer'],
  },
}

// 从 query 参数获取目标路径
const targetPath = computed(() => (route.query.target as string) || '')
const guide = computed<GuideConfig>(() => {
  // 从路径中提取页面名称，如 /plugins/ai-design-assistant/create → create
  const pageName = targetPath.value.replace('/plugins/ai-design-assistant/', '').split('/')[0]
  return GUIDE_MAP[pageName] || {
    icon: '🔒',
    title: '功能需要特定角色',
    desc: '当前角色无权访问此功能，请邀请对应角色的成员加入工作室。',
    targetRoles: [],
  }
})

function goInvite() {
  router.push('/plugins/ai-design-assistant/settings')
}

function goBack() {
  // 回到管理员默认页
  const identityRole = localStorage.getItem('ai_design_role')
  if (identityRole === 'boss' || identityRole === 'merchant_owner' || identityRole === 'tenant_admin') {
    router.push('/plugins/ai-design-assistant/board')
  } else {
    router.push('/plugins/ai-design-assistant/list')
  }
}
</script>

<style scoped>
.guide-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  padding: 40px 20px;
}

.guide-card {
  max-width: 520px;
  width: 100%;
  text-align: center;
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: calc(var(--radius) + 4px);
  padding: 48px 40px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
}

.guide-icon {
  font-size: 56px;
  margin-bottom: 16px;
}

.guide-title {
  font-size: 22px;
  font-weight: 600;
  color: hsl(var(--foreground));
  margin: 0 0 12px;
}

.guide-desc {
  font-size: 14px;
  color: hsl(var(--muted-foreground));
  line-height: 1.7;
  margin: 0 0 24px;
}

.guide-roles {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 28px;
}

.guide-roles-label {
  font-size: 13px;
  color: hsl(var(--muted-foreground));
}

.role-tag {
  display: inline-block;
  padding: 3px 10px;
  background: hsl(var(--primary) / 0.1);
  color: hsl(var(--primary));
  border-radius: calc(var(--radius) - 4px);
  font-size: 13px;
  font-weight: 500;
}

.guide-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-bottom: 20px;
}

.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 24px;
  background: hsl(var(--primary));
  color: hsl(var(--primary-foreground));
  border: none;
  border-radius: calc(var(--radius) - 2px);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.2s;
  font-family: inherit;
}

.btn-primary:hover {
  opacity: 0.9;
}

.btn-secondary {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 24px;
  background: hsl(var(--secondary));
  color: hsl(var(--foreground));
  border: 1px solid hsl(var(--border));
  border-radius: calc(var(--radius) - 2px);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
  font-family: inherit;
}

.btn-secondary:hover {
  background: hsl(var(--accent));
}

.guide-hint {
  padding-top: 16px;
  border-top: 1px solid hsl(var(--border));
}

.guide-hint p {
  margin: 0;
  font-size: 13px;
  color: hsl(var(--muted-foreground));
  line-height: 1.6;
}
</style>
