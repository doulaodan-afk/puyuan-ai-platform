<template>
  <section class="team-settings-page">
    <div class="page-header">
      <h1>成员管理</h1>
      <el-button type="primary" @click="showInviteDialog = true">
        <el-icon><Plus /></el-icon>
        邀请成员
      </el-button>
    </div>

    <div class="members-section">
      <el-table :data="members" v-loading="loading" stripe>
        <el-table-column label="成员" min-width="200">
          <template #default="{ row }">
            <div class="member-cell">
              <div class="avatar">{{ row.nickname?.charAt(0) || '?' }}</div>
              <div class="member-info">
                <div class="name">{{ row.nickname || '未设置' }}</div>
                <div class="contact">{{ row.mobile || '-' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="角色" width="150">
          <template #default="{ row }">
            <el-select
              v-model="row.role"
              :disabled="row.userId === currentUserId"
              @change="handleRoleChange(row)"
              size="small"
            >
              <el-option label="设计师" value="designer" />
              <el-option label="设计助理" value="design_assistant" />
              <el-option label="版师" value="pattern_maker" />
              <el-option label="面料特供商" value="operator" />
              <el-option label="查看者" value="viewer" />
            </el-select>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'info'" size="small">
              {{ row.status === 'active' ? '活跃' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="joinedAt" label="加入时间" width="160">
          <template #default="{ row }">
            {{ formatDate(row.joinedAt) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.userId !== currentUserId"
              type="danger"
              link
              size="small"
              @click="handleRemove(row)"
            >
              移除
            </el-button>
            <span v-else class="current-user-text">当前用户</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="showInviteDialog" title="邀请成员" width="400px">
      <el-form :model="inviteForm" label-width="80px">
        <el-form-item label="手机号" required>
          <el-input v-model="inviteForm.mobile" placeholder="请输入手机号" />
        </el-form-item>

        <el-form-item label="角色" required>
          <el-select v-model="inviteForm.role" placeholder="请选择角色" style="width: 100%">
            <el-option label="设计师" value="designer" />
            <el-option label="设计助理" value="design_assistant" />
            <el-option label="版师" value="pattern_maker" />
            <el-option label="面料特供商" value="operator" />
            <el-option label="查看者" value="viewer" />
          </el-select>
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="inviteForm.remark" type="textarea" :rows="2" placeholder="可选备注" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showInviteDialog = false">取消</el-button>
        <el-button type="primary" @click="handleInvite" :loading="inviting">
          发送邀请
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { generateUUID } from '@/utils/uuid'

interface MemberInfo {
  userId: number
  nickname: string
  mobile: string
  role: string
  status: string
  inviterName?: string
  joinedAt?: string
}

const auth = useAuthStore()

const members = ref<MemberInfo[]>([])
const loading = ref(false)
const showInviteDialog = ref(false)
const inviting = ref(false)
const currentUserId = ref<number | null>(null)

const inviteForm = ref({
  mobile: '',
  role: '',
  remark: '',
})

onMounted(async () => {
  // 从 token 解析当前用户 ID（token 格式: token-{userId}-{tenantId}）
  const tokenRaw = auth.accessToken.replace('Bearer ', '')
  const tokenParts = tokenRaw.split('-')
  currentUserId.value = tokenParts.length >= 2 ? Number(tokenParts[1]) : null

  await loadMembers()
})

function getAuthHeaders(): Record<string, string> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'X-Request-Id': generateUUID(),
    'X-Tenant-Id': auth.tenantId,
  }
  if (auth.accessToken) {
    headers['Authorization'] = `Bearer ${auth.accessToken}`
  }
  if (currentUserId.value) {
    headers['X-User-Id'] = String(currentUserId.value)
  }
  return headers
}

async function loadMembers() {
  loading.value = true
  try {
    const response = await fetch('/api/tenant/members', {
      headers: getAuthHeaders(),
    })
    const payload = await response.json()
    if (payload.code !== 0) {
      throw new Error(payload.message || '获取成员列表失败')
    }
    const rawList: any[] = payload.data ?? []
    members.value = rawList
      .filter((item: any) => item.status === 'active')
      .map((item: any) => ({
        userId: item.user_id ?? item.userId,
        nickname: item.nickname || '未设置',
        mobile: item.mobile || '-',
        role: item.role ?? item.role_code,
        status: item.status || 'active',
        inviterName: item.inviter_name ?? item.inviterName,
        joinedAt: item.joined_at ?? item.joinedAt,
      }))
  } catch (error: any) {
    ElMessage.error('加载成员列表失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

async function handleInvite() {
  if (!inviteForm.value.mobile) {
    ElMessage.warning('请输入手机号')
    return
  }
  if (!inviteForm.value.role) {
    ElMessage.warning('请选择角色')
    return
  }

  inviting.value = true
  try {
    const response = await fetch('/api/tenant/invite', {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({
        mobile: inviteForm.value.mobile,
        role: inviteForm.value.role,
      }),
    })
    const payload = await response.json()
    if (payload.code === 0) {
      ElMessage.success(payload.message || '邀请成功')
      showInviteDialog.value = false
      inviteForm.value = { mobile: '', role: '', remark: '' }
      await loadMembers()
    } else {
      ElMessage.error(payload.message || '邀请失败')
    }
  } catch (error: any) {
    ElMessage.error('邀请失败: ' + (error.message || '未知错误'))
  } finally {
    inviting.value = false
  }
}

async function handleRoleChange(member: MemberInfo) {
  try {
    const response = await fetch(`/api/tenant/members/${member.userId}/role`, {
      method: 'PUT',
      headers: getAuthHeaders(),
      body: JSON.stringify({ role_code: member.role }),
    })
    const payload = await response.json()
    if (payload.code === 0) {
      ElMessage.success(payload.message || '修改成功')
    } else {
      ElMessage.error(payload.message || '修改失败')
      await loadMembers() // 还原
    }
  } catch (error: any) {
    ElMessage.error('修改角色失败: ' + (error.message || '未知错误'))
    await loadMembers() // 还原
  }
}

async function handleRemove(member: MemberInfo) {
  try {
    await ElMessageBox.confirm(
      `确定要移除成员「${member.nickname}」吗？`,
      '确认移除',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )

    const response = await fetch(`/api/tenant/members/${member.userId}`, {
      method: 'DELETE',
      headers: getAuthHeaders(),
    })
    const payload = await response.json()
    if (payload.code === 0) {
      ElMessage.success(payload.message || '移除成功')
      await loadMembers()
    } else {
      ElMessage.error(payload.message || '移除失败')
    }
  } catch (error: any) {
    if (error !== 'cancel' && error?.message !== 'cancel') {
      ElMessage.error('移除失败: ' + (error.message || '未知错误'))
    }
  }
}

function formatDate(dateStr?: string): string {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN') + ' ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.team-settings-page {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: hsl(var(--foreground));
}

.members-section {
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: var(--radius);
  padding: 20px;
}

/* Element Plus Table 主题适配 */
.members-section :deep(.el-table) {
  --el-table-bg-color: hsl(var(--card));
  --el-table-tr-bg-color: hsl(var(--card));
  --el-table-row-hover-bg-color: hsl(var(--accent));
  --el-table-header-bg-color: hsl(var(--secondary));
  --el-table-header-text-color: hsl(var(--foreground));
  --el-table-text-color: hsl(var(--foreground));
  --el-table-border-color: hsl(var(--border));
  --el-table-current-row-bg-color: hsl(var(--accent));
}

.members-section :deep(.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell) {
  background: hsl(var(--secondary));
}

.member-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: hsl(var(--primary));
  color: hsl(var(--primary-foreground));
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 16px;
}

.member-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.name {
  font-weight: 500;
  color: hsl(var(--foreground));
}

.contact {
  font-size: 13px;
  color: hsl(var(--muted-foreground));
}

.current-user-text {
  color: hsl(var(--muted-foreground));
  font-size: 13px;
}
</style>