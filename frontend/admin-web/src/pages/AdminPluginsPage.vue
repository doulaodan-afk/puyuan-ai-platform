<template>
  <section>
    <header class="row-head">
      <h1>插件管理</h1>
      <button @click="openUploadDialog" class="btn-primary">
        <el-icon><Plus /></el-icon>上传新插件
      </button>
    </header>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

    <table v-if="items.length > 0" class="table">
      <thead>
        <tr>
          <th>插件ID</th>
          <th>名称</th>
          <th>版本</th>
          <th>计费</th>
          <th>AI 模型</th>
          <th>生命周期状态</th>
          <th>创建人</th>
          <th>发布时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in items" :key="item.plugin_id">
          <td>
            <span class="plugin-id">{{ item.plugin_id }}</span>
          </td>
          <td>
            <div class="name-cell">
              <span class="plugin-name">{{ item.name }}</span>
              <span v-if="item.description" class="plugin-desc">{{ item.description }}</span>
            </div>
          </td>
          <td>v{{ item.version }}</td>
          <td>
            <span v-if="item.billing_type === 'token'" class="billing-tag">
              {{ item.default_token_cost }} token/次
            </span>
            <span v-else class="billing-tag">{{ item.billing_type }}</span>
          </td>
          <td>
            <span v-if="item.ai_model" class="model-tag">{{ item.ai_model }}</span>
            <span v-else class="model-tag model-default">默认</span>
          </td>
          <td>
            <span :class="'status-badge status-' + item.lifecycle_status">
              {{ statusText(item.lifecycle_status) }}
            </span>
            <span v-if="item.lifecycle_status === 'gray' && item.gray_tenant_count > 0" class="gray-hint">
              ({{ item.gray_tenant_count }}租户)
            </span>
          </td>
          <td>
            <span v-if="item.created_by" class="creator">{{ formatCreator(item.created_by) }}</span>
            <span v-else class="creator">-</span>
          </td>
          <td>
            <span v-if="item.published_at" class="time">{{ formatTime(item.published_at) }}</span>
            <span v-else class="time">-</span>
          </td>
          <td class="actions">
            <!-- testing -->
            <template v-if="item.lifecycle_status === 'testing'">
              <button @click="openModelDialog(item)" class="btn-action">模型配置</button>
              <button @click="goSandbox(item.plugin_id)" class="btn-action">沙箱测试</button>
              <button @click="publishFull(item.plugin_id)" class="btn-action btn-primary">发布全量</button>
              <button @click="openGrayDialog(item.plugin_id)" class="btn-action">灰度发布</button>
              <button @click="deletePlugin(item.plugin_id)" class="btn-action btn-danger">删除</button>
            </template>
            <!-- enabled -->
            <template v-else-if="item.lifecycle_status === 'enabled'">
              <button @click="openModelDialog(item)" class="btn-action">模型配置</button>
              <button @click="goSandbox(item.plugin_id)" class="btn-action">沙箱测试</button>
              <button @click="openGrayDialog(item.plugin_id)" class="btn-action">灰度降级</button>
              <button @click="offlinePlugin(item.plugin_id)" class="btn-action btn-danger">下架</button>
            </template>
            <!-- disabled -->
            <template v-else-if="item.lifecycle_status === 'disabled'">
              <button @click="openModelDialog(item)" class="btn-action">模型配置</button>
              <button @click="publishFull(item.plugin_id)" class="btn-action btn-primary">重新上架</button>
              <button @click="openGrayDialog(item.plugin_id)" class="btn-action">灰度发布</button>
              <button @click="deletePlugin(item.plugin_id)" class="btn-action btn-danger">删除</button>
            </template>
            <!-- gray -->
            <template v-else-if="item.lifecycle_status === 'gray'">
              <button @click="openModelDialog(item)" class="btn-action">模型配置</button>
              <button @click="goSandbox(item.plugin_id)" class="btn-action">沙箱测试</button>
              <button @click="publishFull(item.plugin_id)" class="btn-action btn-primary">全量发布</button>
              <button @click="openGrayDialog(item.plugin_id)" class="btn-action">调整灰度</button>
              <button @click="offlinePlugin(item.plugin_id)" class="btn-action btn-danger">下架</button>
            </template>
          </td>
        </tr>
      </tbody>
    </table>
    <p v-else class="empty">暂无插件</p>

    <!-- 上传弹窗 -->
    <UploadPluginDialog v-model:visible="uploadDialogVisible" @success="loadPlugins" />

    <!-- 灰度配置弹窗 -->
    <GrayConfigDialog
      v-model:visible="grayDialogVisible"
      :plugin-id="currentPluginId"
      @success="loadPlugins"
    />

    <!-- 模型配置弹窗 -->
    <ModelConfigDialog
      v-model:visible="modelDialogVisible"
      :plugin-id="currentPluginId"
      :current-model="currentModelValue"
      @success="loadPlugins"
    />
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { Plus } from "@element-plus/icons-vue";
import { adminRequest } from "../utils/http";
import UploadPluginDialog from "../components/dialog/UploadPluginDialog.vue";
import GrayConfigDialog from "../components/dialog/GrayConfigDialog.vue";
import ModelConfigDialog from "../components/dialog/ModelConfigDialog.vue";

interface PluginItem {
  plugin_id: string;
  name: string;
  version: string;
  billing_type: string;
  default_token_cost: number;
  ai_model?: string;
  lifecycle_status: string;
  review_status: string;
  gray_tenant_count: number;
  created_by?: number;
  tested_at?: string;
  published_at?: string;
  created_at?: string;
  description?: string;
}

interface PluginListData {
  list: PluginItem[];
}

const router = useRouter();
const loading = ref(false);
const errorMessage = ref("");
const items = ref<PluginItem[]>([]);
const uploadDialogVisible = ref(false);
const grayDialogVisible = ref(false);
const modelDialogVisible = ref(false);
const currentPluginId = ref("");
const currentModelValue = ref("");

async function loadPlugins() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const data = await adminRequest<PluginListData>("/api/v1/admin/plugins?page=1&page_size=50");
    items.value = data.list;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "加载失败";
  } finally {
    loading.value = false;
  }
}

function openUploadDialog() {
  uploadDialogVisible.value = true;
}

function openGrayDialog(pluginId: string) {
  currentPluginId.value = pluginId;
  grayDialogVisible.value = true;
}

function openModelDialog(item: PluginItem) {
  currentPluginId.value = item.plugin_id;
  currentModelValue.value = item.ai_model || "";
  modelDialogVisible.value = true;
}

function goSandbox(pluginId: string) {
  router.push(`/admin/plugins/sandbox/${pluginId}`);
}

async function publishFull(pluginId: string) {
  try {
    await adminRequest(`/api/v1/admin/plugins/${pluginId}/publish-full`, { method: "POST" });
    await loadPlugins();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "发布失败";
  }
}

async function offlinePlugin(pluginId: string) {
  try {
    await adminRequest(`/api/v1/admin/plugins/${pluginId}/offline`, { method: "POST" });
    await loadPlugins();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "下架失败";
  }
}

async function deletePlugin(pluginId: string) {
  if (!confirm(`确定删除插件 ${pluginId} 吗？`)) return;
  try {
    await adminRequest(`/api/v1/admin/plugins/${pluginId}`, { method: "DELETE" });
    await loadPlugins();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "删除失败";
  }
}

function statusText(status: string): string {
  const map: Record<string, string> = {
    testing: "测试中",
    enabled: "已上架",
    disabled: "已下架",
    gray: "灰度中",
  };
  return map[status] || status;
}

function formatCreator(createdBy: number): string {
  return `ID:${createdBy}`;
}

function formatTime(timeStr: string): string {
  if (!timeStr) return "-";
  try {
    const date = new Date(timeStr);
    return date.toLocaleDateString("zh-CN", { year: "numeric", month: "2-digit", day: "2-digit" });
  } catch {
    return timeStr;
  }
}

onMounted(() => {
  void loadPlugins();
});
</script>

<style scoped>
.row-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

h1 {
  font-size: 24px;
  font-weight: 600;
  color: hsl(var(--foreground));
  margin: 0;
}

.error {
  color: hsl(var(--destructive));
  background: hsl(var(--destructive) / 0.1);
  padding: 12px;
  border-radius: calc(var(--radius) - 4px);
  font-size: 14px;
  margin-bottom: 16px;
  border: 1px solid hsl(var(--destructive) / 0.3);
}

.table {
  width: 100%;
  border-collapse: collapse;
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: var(--radius);
  overflow: hidden;
}

thead {
  background: hsl(var(--secondary));
}

th {
  padding: 12px 16px;
  text-align: left;
  font-size: 13px;
  font-weight: 600;
  color: hsl(var(--foreground));
  border-bottom: 2px solid hsl(var(--border));
}

td {
  padding: 12px 16px;
  text-align: left;
  font-size: 13px;
  color: hsl(var(--foreground));
  border-bottom: 1px solid hsl(var(--border) / 0.5);
}

tbody tr:hover {
  background: hsl(var(--accent));
}

tbody tr:last-child td {
  border-bottom: none;
}

.plugin-id {
  font-family: monospace;
  font-size: 12px;
  background: hsl(var(--accent));
  padding: 2px 6px;
  border-radius: calc(var(--radius) - 6px);
  color: hsl(var(--primary));
}

.name-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.plugin-name {
  font-weight: 500;
}

.plugin-desc {
  font-size: 12px;
  color: hsl(var(--muted-foreground));
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.billing-tag {
  font-size: 12px;
  background: hsl(var(--primary) / 0.1);
  color: hsl(var(--primary));
  padding: 2px 8px;
  border-radius: calc(var(--radius) - 6px);
}

.model-tag {
  font-size: 12px;
  background: hsl(260 60% 60% / 0.1);
  color: hsl(260 60% 60%);
  padding: 2px 8px;
  border-radius: calc(var(--radius) - 6px);
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: inline-block;
}

.model-default {
  background: hsl(var(--secondary));
  color: hsl(var(--muted-foreground));
}

.status-badge {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-testing {
  background: hsl(38 92% 50% / 0.15);
  color: hsl(38 92% 50%);
  border: 1px solid hsl(38 92% 50% / 0.3);
}

.status-enabled {
  background: hsl(142 71% 45% / 0.15);
  color: hsl(142 71% 45%);
  border: 1px solid hsl(142 71% 45% / 0.3);
}

.status-disabled {
  background: hsl(var(--destructive) / 0.15);
  color: hsl(var(--destructive));
  border: 1px solid hsl(var(--destructive) / 0.3);
}

.status-gray {
  background: hsl(260 60% 60% / 0.15);
  color: hsl(260 60% 60%);
  border: 1px solid hsl(260 60% 60% / 0.3);
}

.gray-hint {
  font-size: 11px;
  color: hsl(var(--muted-foreground));
  margin-left: 4px;
}

.creator {
  font-size: 12px;
  color: hsl(var(--muted-foreground));
}

.time {
  font-size: 12px;
  color: hsl(var(--muted-foreground));
}

.actions {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

button {
  border: none;
  border-radius: calc(var(--radius) - 4px);
  padding: 6px 10px;
  background: hsl(var(--secondary));
  color: hsl(var(--foreground));
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

button:hover:not(:disabled) {
  opacity: 0.85;
}

button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-primary {
  background: hsl(var(--primary));
  color: hsl(var(--primary-foreground));
}

.btn-danger {
  color: hsl(var(--destructive));
  background: hsl(var(--destructive) / 0.1);
}

.btn-danger:hover:not(:disabled) {
  background: hsl(var(--destructive) / 0.2);
}

.btn-action {
  font-size: 12px;
  padding: 4px 8px;
}

.empty {
  color: hsl(var(--muted-foreground));
  text-align: center;
  padding: 48px 24px;
  font-size: 14px;
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: var(--radius);
}
</style>