<template>
  <section class="page-container">
    <header class="row-head">
      <h1>面料商入驻审核</h1>
    </header>

    <div class="filter-bar">
      <div class="filters">
        <select v-model="statusFilter" @change="reloadFirstPage">
          <option value="">全部状态</option>
          <option value="pending">待审核</option>
          <option value="approved">已通过</option>
          <option value="rejected">已驳回</option>
        </select>
        <button @click="reloadFirstPage" :disabled="loading">{{ loading ? "加载中..." : "查询" }}</button>
      </div>
    </div>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

    <table v-if="items.length > 0" class="table">
      <thead>
        <tr>
          <th>ID</th>
          <th>公司名称</th>
          <th>联系人</th>
          <th>手机号</th>
          <th>面料品类</th>
          <th>状态</th>
          <th>申请时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in items" :key="item.id">
          <td>{{ item.id }}</td>
          <td>{{ item.companyName }}</td>
          <td>{{ item.contactName }}</td>
          <td>{{ item.contactMobile }}</td>
          <td>
            <span v-for="(cat, idx) in item.fabricCategories" :key="idx" class="tag">
              {{ cat }}
            </span>
          </td>
          <td>
            <span :class="'status-' + item.status">
              {{ getStatusText(item.status) }}
            </span>
          </td>
          <td>{{ formatDate(item.createdAt) }}</td>
          <td>
            <button v-if="item.status === 'pending'" @click="openReviewDialog(item)" class="btn-primary">
              审核
            </button>
            <span v-else class="disabled">已处理</span>
          </td>
        </tr>
      </tbody>
    </table>
    <p v-else class="empty">暂无入驻申请</p>

    <footer class="pager">
      <button @click="prevPage" :disabled="page <= 1 || loading">上一页</button>
      <span>第 {{ page }} 页 / 共 {{ totalPages }} 页</span>
      <button @click="nextPage" :disabled="page >= totalPages || loading">下一页</button>
    </footer>

    <!-- 审核对话框 -->
    <div v-if="reviewDialogVisible" class="modal-overlay" @click.self="closeReviewDialog">
      <div class="modal">
        <div class="modal-header">
          <h2>面料商入驻审核</h2>
          <button @click="closeReviewDialog" class="close-btn">
            <el-icon><Close /></el-icon>
          </button>
        </div>
        <div class="modal-body">
          <div class="detail-row">
            <label>公司名称：</label>
            <span>{{ currentItem?.companyName }}</span>
          </div>
          <div class="detail-row">
            <label>联系人：</label>
            <span>{{ currentItem?.contactName }}</span>
          </div>
          <div class="detail-row">
            <label>手机号：</label>
            <span>{{ currentItem?.contactMobile }}</span>
          </div>
          <div class="detail-row">
            <label>地址：</label>
            <span>{{ currentItem?.address || "未填写" }}</span>
          </div>
          <div class="detail-row">
            <label>面料品类：</label>
            <div class="tags">
              <span v-for="(cat, idx) in currentItem?.fabricCategories" :key="idx" class="tag">
                {{ cat }}
              </span>
            </div>
          </div>
          <div class="detail-row">
            <label>公司介绍：</label>
            <span>{{ currentItem?.description || "未填写" }}</span>
          </div>

          <div class="review-section">
            <label>审核结果：</label>
            <div class="radio-group">
              <label>
                <input type="radio" v-model="reviewAction" value="approve" />
                通过
              </label>
              <label>
                <input type="radio" v-model="reviewAction" value="reject" />
                驳回
              </label>
            </div>
          </div>

          <div v-if="reviewAction === 'reject'" class="reject-reason">
            <label>驳回原因：</label>
            <textarea v-model="rejectReason" placeholder="请输入驳回原因" rows="3"></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button @click="closeReviewDialog">取消</button>
          <button @click="submitReview" :disabled="submitting" class="btn-primary">
            {{ submitting ? "提交中..." : "确认" }}
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { Close } from "@element-plus/icons-vue";
import { adminRequest } from "../utils/http";

interface RegistrationItem {
  id: number;
  companyName: string;
  contactName: string;
  contactMobile: string;
  fabricCategories: string[];
  address?: string;
  description?: string;
  status: string;
  createdAt: string;
}

interface ReviewPageData {
  registrations: RegistrationItem[];
  total: number;
}

const loading = ref(false);
const errorMessage = ref("");
const statusFilter = ref("");
const items = ref<RegistrationItem[]>([]);
const page = ref(1);
const pageSize = 10;
const total = ref(0);

const reviewDialogVisible = ref(false);
const currentItem = ref<RegistrationItem | null>(null);
const reviewAction = ref<"approve" | "reject">("approve");
const rejectReason = ref("");
const submitting = ref(false);

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)));

async function loadRegistrations() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const query = new URLSearchParams({
      page: String(page.value),
      size: String(pageSize),
    });
    if (statusFilter.value) {
      query.set("status", statusFilter.value);
    }

    const data = await adminRequest<ReviewPageData>(
      `/api/admin/supplier/registrations?${query.toString()}`
    );
    items.value = data.registrations;
    total.value = data.total;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "加载失败";
  } finally {
    loading.value = false;
  }
}

async function submitReview() {
  if (!currentItem.value) return;

  if (reviewAction.value === "reject" && !rejectReason.value.trim()) {
    errorMessage.value = "驳回时必须填写驳回原因";
    return;
  }

  submitting.value = true;
  errorMessage.value = "";
  try {
    await adminRequest(`/api/admin/supplier/review/${currentItem.value.id}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        action: reviewAction.value,
        rejectReason: rejectReason.value,
      }),
    });
    closeReviewDialog();
    await loadRegistrations();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "审核失败";
  } finally {
    submitting.value = false;
  }
}

function openReviewDialog(item: RegistrationItem) {
  currentItem.value = item;
  reviewAction.value = "approve";
  rejectReason.value = "";
  reviewDialogVisible.value = true;
}

function closeReviewDialog() {
  reviewDialogVisible.value = false;
  currentItem.value = null;
  reviewAction.value = "approve";
  rejectReason.value = "";
}

function reloadFirstPage() {
  page.value = 1;
  void loadRegistrations();
}

function prevPage() {
  if (page.value > 1) {
    page.value -= 1;
    void loadRegistrations();
  }
}

function nextPage() {
  if (page.value < totalPages.value) {
    page.value += 1;
    void loadRegistrations();
  }
}

function getStatusText(status: string): string {
  const statusMap: Record<string, string> = {
    pending: "待审核",
    approved: "已通过",
    rejected: "已驳回",
  };
  return statusMap[status] || status;
}

function formatDate(dateStr: string): string {
  const date = new Date(dateStr);
  return date.toLocaleString("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

onMounted(() => {
  void loadRegistrations();
});
</script>

<style scoped>
.row-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 12px;
}

.row-head h1 {
  font-size: 24px;
  font-weight: 600;
  color: hsl(var(--foreground));
  margin: 0;
}

.error {
  color: hsl(var(--destructive));
  background: hsl(var(--destructive) / 0.1);
  padding: 12px 16px;
  border-radius: calc(var(--radius) - 4px);
  margin-bottom: 16px;
  border: 1px solid hsl(var(--destructive) / 0.3);
  font-size: 14px;
}

.filters {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-bar {
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: var(--radius);
  padding: 16px 20px;
  margin-bottom: 20px;
}

select {
  padding: 8px 12px;
  border: 1px solid hsl(var(--input));
  border-radius: calc(var(--radius) - 4px);
  font-size: 14px;
  color: hsl(var(--foreground));
  background: hsl(var(--background));
  cursor: pointer;
  transition: border-color 0.2s ease;
}

select:hover {
  border-color: hsl(var(--ring));
}

select:focus {
  outline: none;
  border-color: hsl(var(--ring));
  box-shadow: 0 0 0 3px hsl(var(--ring) / 0.1);
}

.table {
  margin-top: 16px;
  width: 100%;
  border-collapse: collapse;
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: var(--radius);
  overflow: hidden;
}

th,
td {
  border-bottom: 1px solid hsl(var(--border) / 0.5);
  padding: 12px 16px;
  text-align: left;
  font-size: 14px;
  color: hsl(var(--foreground));
}

th {
  background: hsl(var(--secondary));
  font-weight: 600;
  font-size: 13px;
  color: hsl(var(--foreground));
}

tbody tr:hover {
  background: hsl(var(--accent));
}

tbody tr:last-child td {
  border-bottom: none;
}

.tag {
  display: inline-block;
  background: hsl(var(--accent-blue) / 0.15);
  color: hsl(var(--accent-blue));
  padding: 2px 8px;
  border-radius: calc(var(--radius) - 6px);
  font-size: 12px;
  margin-right: 4px;
  margin-bottom: 2px;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.status-pending {
  color: hsl(38 92% 50%);
  font-weight: 500;
}

.status-approved {
  color: hsl(142 71% 45%);
  font-weight: 500;
}

.status-rejected {
  color: hsl(var(--destructive));
  font-weight: 500;
}

.disabled {
  color: hsl(var(--muted-foreground));
  font-size: 13px;
}

.pager {
  margin-top: 16px;
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: center;
}

.pager span {
  font-size: 14px;
  color: hsl(var(--muted-foreground));
}

button {
  border: none;
  border-radius: calc(var(--radius) - 4px);
  padding: 8px 16px;
  background: hsl(var(--primary));
  color: hsl(var(--primary-foreground));
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

button:hover:not(:disabled) {
  opacity: 0.9;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-primary {
  background: hsl(var(--primary));
}

.btn-primary:hover:not(:disabled) {
  opacity: 0.9;
}

.empty {
  color: hsl(var(--muted-foreground));
  font-size: 14px;
  text-align: center;
  padding: 48px 0;
}

/* Modal styles */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: hsl(0 0% 0% / 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

.modal {
  background: hsl(var(--card));
  border-radius: var(--radius);
  max-width: 600px;
  width: 100%;
  max-height: 90vh;
  overflow-y: auto;
  border: 1px solid hsl(var(--border));
  box-shadow: var(--shadow-lg);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid hsl(var(--border));
}

.modal-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: hsl(var(--foreground));
}

.close-btn {
  background: none;
  border: none;
  padding: 4px;
  color: hsl(var(--muted-foreground));
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: calc(var(--radius) - 6px);
  font-size: 18px;
}

.close-btn:hover {
  background: hsl(var(--accent));
  color: hsl(var(--foreground));
}

.modal-body {
  padding: 20px;
}

.detail-row {
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-row label {
  font-weight: 500;
  color: hsl(var(--muted-foreground));
  font-size: 13px;
}

.detail-row span {
  color: hsl(var(--foreground));
  font-size: 14px;
}

.review-section {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid hsl(var(--border));
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.review-section label {
  font-weight: 500;
  color: hsl(var(--muted-foreground));
  font-size: 13px;
}

.radio-group {
  display: flex;
  gap: 16px;
}

.radio-group label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 400;
  cursor: pointer;
  color: hsl(var(--foreground));
  font-size: 14px;
}

.radio-group input[type="radio"] {
  margin: 0;
  cursor: pointer;
  accent-color: hsl(var(--primary));
}

.reject-reason {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.reject-reason label {
  font-weight: 500;
  color: hsl(var(--muted-foreground));
  font-size: 13px;
}

.reject-reason textarea {
  padding: 10px 12px;
  border: 1px solid hsl(var(--input));
  border-radius: calc(var(--radius) - 4px);
  font-size: 14px;
  color: hsl(var(--foreground));
  background: hsl(var(--background));
  resize: vertical;
  font-family: inherit;
  transition: border-color 0.2s ease;
}

.reject-reason textarea:hover {
  border-color: hsl(var(--ring));
}

.reject-reason textarea:focus {
  outline: none;
  border-color: hsl(var(--ring));
  box-shadow: 0 0 0 3px hsl(var(--ring) / 0.1);
}

.modal-footer {
  padding: 16px 20px;
  border-top: 1px solid hsl(var(--border));
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.modal-footer button {
  min-width: 80px;
}
</style>