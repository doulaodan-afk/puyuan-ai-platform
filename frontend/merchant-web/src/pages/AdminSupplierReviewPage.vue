<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

interface RegistrationItem {
  id: number
  companyName: string
  contactName: string
  contactMobile: string
  fabricCategories: string[]
  status: string
  createdAt: string
}

const loading = ref(false)
const registrations = ref<RegistrationItem[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)

const reviewDialogVisible = ref(false)
const currentRegistration = ref<RegistrationItem | null>(null)
const reviewAction = ref<'approve' | 'reject'>('approve')
const rejectReason = ref('')

async function loadRegistrations() {
  loading.value = true
  try {
    const response = await fetch(`/api/admin/supplier/registrations?page=${currentPage.value}&size=${pageSize.value}`)
    const result = await response.json()
    if (result.code === 0) {
      registrations.value = result.data.registrations
      total.value = result.data.total
    }
  } catch (error) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function openReviewDialog(registration: RegistrationItem) {
  currentRegistration.value = registration
  reviewAction.value = 'approve'
  rejectReason.value = ''
  reviewDialogVisible.value = true
}

async function submitReview() {
  if (reviewAction.value === 'reject' && !rejectReason.value) {
    ElMessage.warning('请填写驳回原因')
    return
  }

  try {
    const response = await fetch(`/api/admin/supplier/registration/${currentRegistration.value!.id}/review`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'X-User-Id': '1'
      },
      body: JSON.stringify({
        action: reviewAction.value,
        rejectReason: rejectReason.value
      })
    })
    const result = await response.json()
    if (result.code === 0) {
      ElMessage.success(result.data.message)
      reviewDialogVisible.value = false
      loadRegistrations()
    } else {
      ElMessage.error(result.message || '操作失败')
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

function handlePageChange(page: number) {
  currentPage.value = page
  loadRegistrations()
}

onMounted(() => {
  loadRegistrations()
})
</script>

<template>
  <div class="admin-supplier-review-page page-container">
    <div class="page-header">
      <h1>面料商入驻审核</h1>
    </div>

    <el-table :data="registrations" v-loading="loading" stripe>
      <el-table-column prop="companyName" label="公司名称" width="180" />
      <el-table-column prop="contactName" label="联系人" width="120" />
      <el-table-column prop="contactMobile" label="手机号" width="130" />
      <el-table-column label="面料品类" width="200">
        <template #default="{ row }">
          <el-tag
            v-for="(category, index) in row.fabricCategories"
            :key="index"
            size="small"
            style="margin-right: 4px"
          >
            {{ category }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'approved' ? 'success' : row.status === 'rejected' ? 'danger' : 'warning'">
            {{ row.status === 'approved' ? '已通过' : row.status === 'rejected' ? '已驳回' : '待审核' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="申请时间" width="180" />
      <el-table-column label="操作" fixed="right" width="200">
        <template #default="{ row }">
          <template v-if="row.status === 'pending'">
            <el-button type="primary" size="small" @click="openReviewDialog(row)">
              通过
            </el-button>
            <el-button type="danger" size="small" @click="openReviewDialog(row)">
              驳回
            </el-button>
          </template>
          <span v-else style="color: #9ca3af">已处理</span>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="currentPage"
      :page-size="pageSize"
      :total="total"
      @current-change="handlePageChange"
      layout="total, prev, pager, next"
      style="margin-top: 20px; justify-content: center"
    />

    <el-dialog v-model="reviewDialogVisible" title="审核入驻申请" width="500px">
      <div v-if="currentRegistration" class="review-content">
        <p><strong>公司名称：</strong>{{ currentRegistration.companyName }}</p>
        <p><strong>联系人：</strong>{{ currentRegistration.contactName }}</p>
        <p><strong>手机号：</strong>{{ currentRegistration.contactMobile }}</p>

        <el-radio-group v-model="reviewAction" style="margin: 20px 0">
          <el-radio label="approve">通过</el-radio>
          <el-radio label="reject">驳回</el-radio>
        </el-radio-group>

        <div v-if="reviewAction === 'reject'" style="margin-top: 16px">
          <el-input
            v-model="rejectReason"
            type="textarea"
            :rows="4"
            placeholder="请输入驳回原因"
          />
        </div>
      </div>

      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReview">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin-supplier-review-page {
  padding: 24px;
  background: #f9fafb;
  min-height: calc(100vh - 80px);
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0;
  font-size: 24px;
  color: #1f2937;
}

.review-content p {
  margin: 8px 0;
  color: #374151;
}
</style>