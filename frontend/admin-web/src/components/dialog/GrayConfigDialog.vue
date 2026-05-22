<template>
  <div v-if="visible" class="modal-overlay" @click.self="close">
    <div class="modal" style="max-width: 680px;">
      <div class="modal-header">
        <h2>灰度发布配置</h2>
        <button @click="close" class="close-btn">
          <el-icon><Close /></el-icon>
        </button>
      </div>
      <div class="modal-body">
        <!-- 模式切换 -->
        <div class="mode-tabs">
          <button
            :class="{ active: mode === 'manual' }"
            @click="mode = 'manual'"
          >手动选择租户</button>
          <button
            :class="{ active: mode === 'percentage' }"
            @click="switchToPercentageMode"
          >按百分比</button>
        </div>

        <!-- 手动选择模式 -->
        <template v-if="mode === 'manual'">
          <div class="search-bar">
            <input v-model="keyword" placeholder="搜索租户名称..." @input="onSearchInput" class="search-input" />
          </div>

          <div class="tenant-list">
            <div v-if="loading" class="loading-msg">加载中...</div>
            <div v-else-if="tenants.length === 0" class="empty-msg">暂无租户</div>
            <div v-else>
              <label v-for="t in tenants" :key="t.tenant_id" class="tenant-item">
                <input type="checkbox" :value="t.tenant_id" v-model="selectedIds" />
                <span class="tenant-name">{{ t.tenant_name }}</span>
                <span class="tenant-code">{{ t.tenant_code }}</span>
              </label>
            </div>
          </div>

          <div v-if="totalPages > 1" class="pager">
            <button @click="prevPage" :disabled="page <= 1 || loading">上一页</button>
            <span>{{ page }} / {{ totalPages }}</span>
            <button @click="nextPage" :disabled="page >= totalPages || loading">下一页</button>
          </div>

          <div v-if="selectedIds.length > 0" class="selected-bar">
            已选 {{ selectedIds.length }} 个租户
          </div>
        </template>

        <!-- 百分比模式 -->
        <template v-else>
          <div class="percentage-section">
            <div class="percentage-input-row">
              <label>选择灰度比例：</label>
              <div class="percentage-slider">
                <input
                  type="range"
                  min="1"
                  max="100"
                  v-model.number="percentageValue"
                  class="slider"
                />
                <span class="percentage-label">{{ percentageValue }}%</span>
              </div>
            </div>
            <div class="percentage-info">
              <p>当前租户总数：<strong>{{ totalTenants }}</strong></p>
              <p>预计灰度租户数：<strong>{{ Math.ceil(totalTenants * percentageValue / 100) }}</strong></p>
              <p v-if="percentageValue >= 50" class="warning-hint">⚠️ 比例超过 50%，建议使用全量发布</p>
            </div>
            <div class="percentage-actions">
              <button @click="applyPercentage" :disabled="applyingPercentage" class="btn-apply">
                {{ applyingPercentage ? "应用中..." : "应用此比例" }}
              </button>
              <button @click="clearPercentage" class="btn-clear">清除</button>
            </div>
            <div v-if="selectedIds.length > 0" class="selected-bar">
              已自动选择 {{ selectedIds.length }} 个租户
            </div>
          </div>
        </template>

        <div v-if="errorMessage" class="error-msg">{{ errorMessage }}</div>
      </div>
      <div class="modal-footer">
        <button @click="close" :disabled="submitting">取消</button>
        <button @click="confirmGray" :disabled="selectedIds.length === 0 || submitting" class="btn-primary">
          {{ submitting ? "提交中..." : `确认灰度 (${selectedIds.length})` }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from "vue";
import { Close } from "@element-plus/icons-vue";
import { grayPublish, listTenants, type TenantItem } from "../../api/plugin";

const props = defineProps<{ visible: boolean; pluginId: string }>();
const emit = defineEmits<{
  (e: "update:visible", v: boolean): void;
  (e: "success"): void;
}>();

const mode = ref<"manual" | "percentage">("manual");
const keyword = ref("");
const tenants = ref<TenantItem[]>([]);
const selectedIds = ref<number[]>([]);
const page = ref(1);
const pageSize = 20;
const total = ref(0);
const totalTenants = ref(0);
const loading = ref(false);
const submitting = ref(false);
const errorMessage = ref("");
const percentageValue = ref(10);
const applyingPercentage = ref(false);

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)));

watch(() => props.visible, async (v) => {
  if (v) {
    mode.value = "manual";
    page.value = 1;
    selectedIds.value = [];
    keyword.value = "";
    errorMessage.value = "";
    percentageValue.value = 10;
    await loadTotalTenants();
    await loadTenants();
  }
});

async function loadTotalTenants() {
  try {
    const data = await listTenants(1, 1);
    totalTenants.value = data.total;
  } catch {
    totalTenants.value = 0;
  }
}

async function loadTenants() {
  loading.value = true;
  try {
    const data = await listTenants(page.value, pageSize, keyword.value || undefined);
    tenants.value = data.list;
    total.value = data.total;
  } catch {
    tenants.value = [];
  } finally {
    loading.value = false;
  }
}

function switchToPercentageMode() {
  mode.value = "percentage";
  selectedIds.value = [];
}

let searchTimer: ReturnType<typeof setTimeout>;
function onSearchInput() {
  clearTimeout(searchTimer);
  searchTimer = setTimeout(() => {
    page.value = 1;
    void loadTenants();
  }, 300);
}

function prevPage() {
  if (page.value > 1) {
    page.value -= 1;
    void loadTenants();
  }
}

function nextPage() {
  if (page.value < totalPages.value) {
    page.value += 1;
    void loadTenants();
  }
}

function close() {
  emit("update:visible", false);
}

async function applyPercentage() {
  if (totalTenants.value === 0) {
    errorMessage.value = "无法获取租户总数";
    return;
  }

  applyingPercentage.value = true;
  errorMessage.value = "";
  try {
    // 获取所有租户（分页加载直到获取足够的数量）
    const targetCount = Math.ceil(totalTenants.value * percentageValue.value / 100);
    let allTenants: TenantItem[] = [];
    let currentPage = 1;
    let fetchMore = true;

    while (fetchMore && allTenants.length < targetCount * 1.5) {
      const data = await listTenants(currentPage, 100);
      if (data.list.length === 0) {
        fetchMore = false;
      } else {
        allTenants = allTenants.concat(data.list);
        if (data.list.length < 100) {
          fetchMore = false;
        } else {
          currentPage++;
        }
      }
    }

    // 随机打乱并选择目标数量
    const shuffled = allTenants.sort(() => Math.random() - 0.5);
    const selected = shuffled.slice(0, targetCount);
    selectedIds.value = selected.map(t => t.tenant_id);

  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : "应用百分比失败";
  } finally {
    applyingPercentage.value = false;
  }
}

function clearPercentage() {
  selectedIds.value = [];
  percentageValue.value = 10;
}

async function confirmGray() {
  if (!props.pluginId || selectedIds.value.length === 0) return;
  submitting.value = true;
  try {
    await grayPublish(props.pluginId, selectedIds.value);
    close();
    emit("success");
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : "配置失败";
  } finally {
    submitting.value = false;
  }
}
</script>

<style scoped>
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
  max-width: 680px;
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
  border-radius: calc(var(--radius) - 6px);
  font-size: 18px;
}

.close-btn:hover {
  background: hsl(var(--accent));
  color: hsl(var(--foreground));
}

.modal-body {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.mode-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.mode-tabs button {
  border: none;
  border-radius: calc(var(--radius) - 4px);
  padding: 8px 16px;
  background: hsl(var(--secondary));
  color: hsl(var(--foreground));
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.mode-tabs button.active {
  background: hsl(var(--primary));
  color: hsl(var(--primary-foreground));
}

.search-bar {
  margin-bottom: 4px;
}

.search-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid hsl(var(--input));
  border-radius: calc(var(--radius) - 4px);
  background: hsl(var(--background));
  color: hsl(var(--foreground));
  font-size: 14px;
  box-sizing: border-box;
}

.search-input:focus {
  outline: none;
  border-color: hsl(var(--ring));
}

.tenant-list {
  max-height: 280px;
  overflow-y: auto;
  border: 1px solid hsl(var(--border));
  border-radius: calc(var(--radius) - 4px);
}

.loading-msg,
.empty-msg {
  padding: 24px;
  text-align: center;
  color: hsl(var(--muted-foreground));
  font-size: 14px;
}

.tenant-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  cursor: pointer;
  border-bottom: 1px solid hsl(var(--border) / 0.5);
  font-size: 14px;
}

.tenant-item:last-child {
  border-bottom: none;
}

.tenant-item:hover {
  background: hsl(var(--accent));
}

.tenant-item input[type="checkbox"] {
  accent-color: hsl(var(--primary));
  cursor: pointer;
}

.tenant-name {
  flex: 1;
  color: hsl(var(--foreground));
}

.tenant-code {
  font-size: 12px;
  color: hsl(var(--muted-foreground));
}

.pager {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: center;
}

.pager button {
  border: none;
  border-radius: calc(var(--radius) - 4px);
  padding: 6px 12px;
  background: hsl(var(--secondary));
  color: hsl(var(--foreground));
  font-size: 13px;
  cursor: pointer;
}

.pager button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.selected-bar {
  text-align: center;
  font-size: 13px;
  color: hsl(var(--primary));
  padding: 6px;
  background: hsl(var(--primary) / 0.1);
  border-radius: calc(var(--radius) - 4px);
}

/* Percentage mode styles */
.percentage-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.percentage-input-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.percentage-input-row label {
  font-size: 14px;
  color: hsl(var(--foreground));
  white-space: nowrap;
}

.percentage-slider {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.slider {
  flex: 1;
  height: 6px;
  border-radius: 3px;
  accent-color: hsl(var(--primary));
  cursor: pointer;
}

.percentage-label {
  font-size: 16px;
  font-weight: 600;
  color: hsl(var(--primary));
  min-width: 50px;
}

.percentage-info {
  background: hsl(var(--accent) / 0.3);
  padding: 12px 16px;
  border-radius: calc(var(--radius) - 4px);
  font-size: 14px;
  color: hsl(var(--foreground));
}

.percentage-info p {
  margin: 4px 0;
}

.percentage-info strong {
  color: hsl(var(--primary));
}

.warning-hint {
  color: hsl(38 92% 50%);
  font-size: 13px;
}

.percentage-actions {
  display: flex;
  gap: 8px;
}

.btn-apply {
  border: none;
  border-radius: calc(var(--radius) - 4px);
  padding: 8px 16px;
  background: hsl(var(--primary));
  color: hsl(var(--primary-foreground));
  font-size: 14px;
  cursor: pointer;
}

.btn-apply:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-clear {
  border: none;
  border-radius: calc(var(--radius) - 4px);
  padding: 8px 16px;
  background: hsl(var(--secondary));
  color: hsl(var(--foreground));
  font-size: 14px;
  cursor: pointer;
}

.error-msg {
  color: hsl(var(--destructive));
  font-size: 13px;
  padding: 8px 12px;
  background: hsl(var(--destructive) / 0.1);
  border-radius: calc(var(--radius) - 4px);
  border: 1px solid hsl(var(--destructive) / 0.3);
}

.modal-footer {
  padding: 16px 20px;
  border-top: 1px solid hsl(var(--border));
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.modal-footer button {
  min-width: 100px;
  border: none;
  border-radius: calc(var(--radius) - 4px);
  padding: 8px 16px;
  background: hsl(var(--primary));
  color: hsl(var(--primary-foreground));
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.2s;
}

.modal-footer button:hover:not(:disabled) {
  opacity: 0.9;
}

.modal-footer button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.modal-footer button:first-child {
  background: hsl(var(--secondary));
  color: hsl(var(--foreground));
}
</style>