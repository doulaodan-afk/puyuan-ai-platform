<template>
  <div class="partner-manage page-container">
    <header class="header">
      <h1>合作方管理</h1>
      <el-button type="primary" @click="inviteDialogVisible = true">
        + 邀请面料商合作
      </el-button>
    </header>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="可合作供应商" name="available">
        <div v-if="loading" v-loading="true" element-loading-text="加载中...">
          <el-table :data="availableSuppliers" stripe>
            <el-table-column prop="tenant_name" label="供应商名称" width="200" />
            <el-table-column prop="fabric_categories" label="面料品类" width="300">
              <template #default="{ row }">
                <el-tag
                  v-for="(category, index) in row.fabric_categories"
                  :key="index"
                  size="small"
                  style="margin-right: 4px"
                >
                  {{ category }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="created_at" label="入驻时间" width="180" />
            <el-table-column label="status" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusTagType(row.status)" size="small">
                  {{ getStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" fixed="right" width="120">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="inviteSupplier(row)">
                  邀请合作
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="availablePage"
            :page-size="20"
            :total="availableTotal"
            @current-change="handleAvailablePageChange"
            layout="total, prev, pager, next"
            style="margin-top: 20px; justify-content: center"
          />
        </div>

        <el-empty v-if="!loading && availableSuppliers.length === 0" description="暂无已入驻供应商"></el-empty>
      </el-tab-pane>

      <el-tab-pane label="我的合作" name="collaborations">
        <div v-if="loading" v-loading="true" element-loading-text="加载中...">
          <el-table :data="collaborations" stripe>
            <el-table-column prop="supplier_name" label="供应商名称" width="180" />
            <el-table-column prop="tenant_name" label="工作室名称" width="180" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusTagType(row.status)" size="small">
                  {{ getStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="inviter_name" label="邀请人" width="120" />
            <el-table-column prop="responded_at" label="响应时间" width="180" />
            <el-table-column prop="created_at" label="邀请时间" width="180" />
            <el-table-column label="操作" fixed="right" width="120">
              <template #default="{ row }">
                <el-button v-if="row.status === 'pending'" type="primary" size="small" @click="respondCollaboration(row, true)">
                  接受
                </el-button>
                <el-button v-if="row.status === 'pending'" type="danger" size="small" @click="respondCollaboration(row, false)">
                  拒绝
                </el-button>
                <span v-else-if="row.status === 'rejected'" class="disabled-text">已拒绝</span>
                <span v-else-if="row.status === 'accepted'" class="disabled-text">已接受</span>
                <el-button v-if="row.status === 'accepted'" type="warning" size="small" @click="openBlockDialog(row)">
                  屏蔽
                </el-button>
                <span v-if="row.status === 'blocked'" class="disabled-text">已屏蔽</span>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="collaborationPage"
            :page-size="20"
            :total="collaborationTotal"
            @current-change="handleCollaborationPageChange"
            layout="total, prev, pager, next"
            style="margin-top: 20px; justify-content: center"
          />
        </div>

        <el-empty v-if="!loading && collaborations.length === 0" description="暂无合作邀请"></el-empty>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="inviteDialogVisible" title="邀请面料商合作" width="500px">
      <el-form :model="inviteForm" label-width="100px">
        <el-form-item label="选择面料商" required>
          <el-select v-model="selectedSupplierId" placeholder="请选择面料商" clearable style="width: 100%">
            <el-option
              v-for="supplier in availableSuppliers"
              :key="supplier.tenant_id"
              :label="supplier.tenant_name"
              :value="supplier.tenant_id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="inviteRemark" placeholder="请填写备注（可选）" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="inviteDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleInvite" :disabled="inviting">发送邀请</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="blockDialogVisible" title="屏蔽供应商" width="500px">
      <div class="block-content">
        <p><strong>供应商：</strong>{{ currentCollaboration?.supplier_name || '未选择' }}</p>
        <div style="margin-top: 16px;">
          <label style="display: block; margin-bottom: 8px;">屏蔽原因：</label>
          <el-input v-model="blockReason" type="textarea" :rows="4" placeholder="请输入屏蔽原因" />
        </div>
      </div>
      <template #footer>
        <el-button @click="blockDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="handleBlock">确认屏蔽</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getAvailableSuppliers,
  getCollaborationList,
  inviteSupplier as inviteSupplierApi,
  respondCollaboration as respondCollaborationApi,
  blockCollaboration as blockCollaborationApi,
} from '../api'
import type { SupplierInfo, CollaborationInfo } from '../api'

const availableSuppliers = ref<SupplierInfo[]>([])
const collaborations = ref<CollaborationInfo[]>([])
const currentCollaboration = ref<CollaborationInfo | null>(null)

const loading = ref(false)
const activeTab = ref('available')
const availablePage = ref(1)
const availableTotal = ref(0)
const collaborationPage = ref(1)
const collaborationTotal = ref(0)

const inviteDialogVisible = ref(false)
const selectedSupplierId = ref<number | null>(null)
const inviteRemark = ref('')
const inviting = ref(false)
const inviteForm = ref({})

const blockDialogVisible = ref(false)
const blockReason = ref('')

onMounted(async () => {
  await loadAvailableSuppliers()
  await loadCollaborations()
})

async function loadAvailableSuppliers() {
  loading.value = true
  try {
    const data = await getAvailableSuppliers({
      page: availablePage.value,
      size: 20,
    })
    availableSuppliers.value = data.suppliers
    availableTotal.value = data.total
  } catch (error: any) {
    ElMessage.error('加载失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

async function loadCollaborations() {
  loading.value = true
  try {
    const data = await getCollaborationList({
      status: activeTab.value === 'pending' ? 'pending' : undefined,
      page: collaborationPage.value,
      size: 20,
    })
    collaborations.value = data.collaborations
    collaborationTotal.value = data.total
  } catch (error: any) {
    ElMessage.error('加载失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

async function inviteSupplier(supplier: SupplierInfo) {
  try {
    inviting.value = true
    const result = await inviteSupplierApi(supplier.tenant_id)
    if (result.success) {
      ElMessage.success('邀请已发送')
      inviteDialogVisible.value = false
      inviteRemark.value = ''
      await loadAvailableSuppliers()
      await loadCollaborations()
    } else {
      ElMessage.error(result.message || '邀请失败')
    }
  } catch (error: any) {
    ElMessage.error('邀请失败: ' + (error.message || '未知错误'))
  } finally {
    inviting.value = false
  }
}

function handleInvite() {
  if (!selectedSupplierId.value) {
    ElMessage.warning('请选择面料商')
    return
  }
  const supplier = availableSuppliers.value.find(
    (s) => s.tenant_id === selectedSupplierId.value
  )
  if (supplier) {
    inviteSupplier(supplier)
  } else {
    inviteSupplier({ tenant_id: selectedSupplierId.value!, tenant_name: '', fabric_categories: [], supplier_name: '', status: '', created_at: '' })
  }
}

async function respondCollaboration(collaboration: CollaborationInfo, accept: boolean) {
  try {
    const result = await respondCollaborationApi(collaboration.id, accept)
    if (result.success) {
      ElMessage.success('操作成功')
      await loadCollaborations()
    } else {
      ElMessage.error(result.message || '操作失败')
    }
  } catch (error: any) {
    ElMessage.error('操作失败: ' + (error.message || '未知错误'))
  }
}

function openBlockDialog(collaboration: CollaborationInfo) {
  currentCollaboration.value = collaboration
  blockReason.value = ''
  blockDialogVisible.value = true
}

async function handleBlock() {
  if (!currentCollaboration.value) return

  try {
    const result = await blockCollaborationApi(currentCollaboration.value.id, blockReason.value)
    if (result.success) {
      ElMessage.success('已屏蔽该供应商')
      blockDialogVisible.value = false
      await loadCollaborations()
    } else {
      ElMessage.error(result.message || '操作失败')
    }
  } catch (error: any) {
    ElMessage.error('操作失败: ' + (error.message || '未知错误'))
  }
}

function getStatusTagType(status: string) {
  switch (status) {
    case 'pending': return 'info'
    case 'accepted': return 'success'
    case 'rejected': return 'danger'
    case 'blocked': return 'warning'
    default: return ''
  }
}

function getStatusText(status: string) {
  switch (status) {
    case 'pending': return '待确认'
    case 'accepted': return '已接受'
    case 'rejected': return '已拒绝'
    case 'blocked': return '已屏蔽'
    default: return status
  }
}

function handleAvailablePageChange(page: number) {
  availablePage.value = page
  loadAvailableSuppliers()
}

function handleCollaborationPageChange(page: number) {
  collaborationPage.value = page
  loadCollaborations()
}
</script>

<style scoped>
.partner-manage {
  padding: 20px;
  background: hsl(var(--card));
  border-radius: 8px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header h1 {
  margin: 0;
  font-size: 24px;
  color: hsl(var(--foreground));
}

.disabled-text {
  color: hsl(var(--muted-foreground));
  font-size: 13px;
}
</style>