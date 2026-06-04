<template>
  <section class="page-container">
    <header class="row-head">
      <h1>AI 提供商管理</h1>
    </header>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

    <!-- 操作栏 -->
    <div class="toolbar">
      <button @click="showAddForm" class="add-btn">添加提供商</button>
    </div>

    <!-- 添加/编辑表单 -->
    <div v-if="showForm" class="form-panel">
      <h3>{{ editingId ? '编辑提供商' : '添加提供商' }}</h3>
      <form class="form-grid" @submit.prevent="saveProvider">
        <div class="form-field">
          <label for="prov-name">标识 (name)</label>
          <input id="prov-name" v-model.trim="form.name" placeholder="如 siliconflow" required />
        </div>
        <div class="form-field">
          <label for="prov-display">显示名称</label>
          <input id="prov-display" v-model.trim="form.displayName" placeholder="如 硅基流动" required />
        </div>
        <div class="form-field">
          <label for="prov-url">Base URL</label>
          <input id="prov-url" v-model.trim="form.baseUrl" placeholder="如 https://api.siliconflow.cn/v1" required />
        </div>
        <div class="form-field">
          <label for="prov-key">API Key</label>
          <input id="prov-key" v-model.trim="form.apiKey" type="password" :placeholder="editingId ? '留空则不修改' : '输入 API Key'" :required="!editingId" />
        </div>
        <div class="form-field">
          <label for="prov-priority">优先级</label>
          <input id="prov-priority" v-model.number="form.priority" type="number" min="0" max="99" />
        </div>
        <div class="form-field">
          <label for="prov-desc">描述</label>
          <input id="prov-desc" v-model.trim="form.description" placeholder="可选" />
        </div>
        <label class="checkbox">
          <input v-model="form.enabled" type="checkbox" />
          启用
        </label>
        <div class="form-actions">
          <button type="submit">{{ editingId ? '更新' : '保存' }}</button>
          <button type="button" @click="cancelForm">取消</button>
        </div>
      </form>
    </div>

    <!-- 提供商列表 -->
    <table v-if="providers.length > 0" class="table">
      <thead>
        <tr>
          <th>ID</th>
          <th>标识</th>
          <th>显示名称</th>
          <th>Base URL</th>
          <th>API Key</th>
          <th>优先级</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="p in providers" :key="p.id">
          <td>{{ p.id }}</td>
          <td>{{ p.name }}</td>
          <td>{{ p.display_name }}</td>
          <td class="url-cell">{{ p.base_url }}</td>
          <td class="masked">{{ p.api_key || '未配置' }}</td>
          <td>{{ p.priority }}</td>
          <td>
            <span :class="p.enabled ? 'status-online' : 'status-offline'">
              {{ p.enabled ? '启用' : '禁用' }}
            </span>
          </td>
          <td class="actions">
            <button @click="editProvider(p)">编辑</button>
            <button @click="testProviderConn(p)">测试</button>
            <button @click="deleteProviderHandler(p)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>
    <p v-else class="empty">暂无 AI 提供商，点击"添加提供商"开始</p>

    <!-- 测试结果对话框 -->
    <div v-if="testResult" class="modal-overlay" @click="closeTestResult">
      <div class="modal" @click.stop>
        <h3>测试结果</h3>
        <p :class="testResult.success ? 'success' : 'error'">
          {{ testResult.success ? '✅' : '❌' }} {{ testResult.message }}
        </p>
        <p v-if="testResult.latency_ms > 0">响应时间: {{ testResult.latency_ms }}ms</p>
        <div v-if="testResult.result" class="test-output">
          <strong>返回内容:</strong>
          <pre>{{ testResult.result }}</pre>
        </div>
        <button @click="closeTestResult">关闭</button>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import {
  type AiProvider,
  type TestModelResponse,
  listProviders,
  createProvider,
  updateProvider,
  deleteProvider,
  testProvider,
} from "../api/aiScene";

const providers = ref<AiProvider[]>([]);
const errorMessage = ref("");
const showForm = ref(false);
const editingId = ref<number | null>(null);
const testResult = ref<TestModelResponse | null>(null);

const form = reactive({
  name: "",
  displayName: "",
  baseUrl: "",
  apiKey: "",
  priority: 0,
  description: "",
  enabled: true,
});

function resetForm() {
  form.name = "";
  form.displayName = "";
  form.baseUrl = "";
  form.apiKey = "";
  form.priority = 0;
  form.description = "";
  form.enabled = true;
}

function showAddForm() {
  showForm.value = true;
  editingId.value = null;
  resetForm();
}

function cancelForm() {
  showForm.value = false;
  editingId.value = null;
}

function editProvider(p: AiProvider) {
  showForm.value = true;
  editingId.value = p.id;
  form.name = p.name;
  form.displayName = p.display_name;
  form.baseUrl = p.base_url;
  form.apiKey = "";
  form.priority = p.priority;
  form.description = p.description || "";
  form.enabled = p.enabled;
}

async function loadProviders() {
  errorMessage.value = "";
  try {
    providers.value = await listProviders();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "加载失败";
  }
}

async function saveProvider() {
  errorMessage.value = "";
  try {
    const payload = {
      name: form.name,
      display_name: form.displayName,
      base_url: form.baseUrl,
      api_key: form.apiKey || undefined,
      priority: form.priority,
      description: form.description || undefined,
      enabled: form.enabled,
    };
    if (editingId.value) {
      await updateProvider(editingId.value, payload);
    } else {
      await createProvider(payload);
    }
    showForm.value = false;
    editingId.value = null;
    await loadProviders();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "保存失败";
  }
}

async function deleteProviderHandler(p: AiProvider) {
  if (!confirm(`确定要删除提供商 "${p.display_name}" 吗？关联的场景绑定也将被删除。`)) return;
  try {
    await deleteProvider(p.id);
    await loadProviders();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "删除失败";
  }
}

async function testProviderConn(p: AiProvider) {
  try {
    testResult.value = await testProvider(p.id);
  } catch (error) {
    testResult.value = {
      success: false,
      message: error instanceof Error ? error.message : "测试失败",
      latency_ms: 0,
    };
  }
}

function closeTestResult() {
  testResult.value = null;
}

onMounted(() => {
  void loadProviders();
});
</script>

<style scoped>
.page-container { max-width: 1200px; margin: 0 auto; padding: 16px; }

.row-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.row-head h1 { font-size: 24px; font-weight: 600; color: hsl(var(--foreground)); }

.error { background: hsl(var(--destructive) / 0.1); color: hsl(var(--destructive)); padding: 12px 16px; border-radius: 8px; margin-bottom: 16px; border: 1px solid hsl(var(--destructive) / 0.2); }
.success { color: hsl(142 71% 45%); font-weight: 500; }

.toolbar { margin-bottom: 16px; }
.add-btn { margin-bottom: 16px; }

.form-panel { background: hsl(var(--muted)); padding: 20px; border-radius: 8px; margin-bottom: 20px; border: 1px solid hsl(var(--border)); }
.form-panel h3 { margin-top: 0; margin-bottom: 16px; font-size: 16px; font-weight: 600; }

.form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px; margin-bottom: 16px; }
.form-field { display: flex; flex-direction: column; gap: 4px; }
.form-field label { font-size: 13px; font-weight: 500; color: hsl(var(--muted-foreground)); }
.form-field input { padding: 10px 14px; border: 1px solid hsl(var(--border)); border-radius: 6px; font-size: 14px; color: hsl(var(--foreground)); background: hsl(var(--background)); }
.checkbox { display: flex; align-items: center; gap: 8px; font-size: 14px; color: hsl(var(--foreground)); }
.form-actions { display: flex; gap: 8px; }

.table { width: 100%; border-collapse: collapse; background: hsl(var(--card)); border: 1px solid hsl(var(--border)); border-radius: 8px; overflow: hidden; }
thead { background: hsl(var(--secondary)); }
th, td { border-bottom: 1px solid hsl(var(--border) / 0.5); padding: 12px; text-align: left; font-size: 14px; color: hsl(var(--foreground)); }
th { font-weight: 600; font-size: 13px; }
tbody tr:hover { background: hsl(var(--accent)); }
.url-cell { max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.masked { font-family: "Consolas", monospace; color: hsl(var(--muted-foreground)); }
.actions { display: flex; gap: 6px; flex-wrap: wrap; }

button { border: none; border-radius: 6px; padding: 8px 16px; background: hsl(var(--primary)); color: hsl(var(--primary-foreground)); font-size: 14px; font-weight: 500; cursor: pointer; }
button:hover:not(:disabled) { opacity: 0.9; }
button[type="button"] { background: hsl(var(--secondary)); color: hsl(var(--secondary-foreground)); }
button[type="button"]:hover:not(:disabled) { background: hsl(var(--accent)); }

.empty { margin-top: 16px; color: hsl(var(--muted-foreground)); font-size: 14px; text-align: center; padding: 40px 0; }

.status-online { color: hsl(142 71% 45%); font-weight: 500; }
.status-offline { color: hsl(var(--destructive)); font-weight: 500; }

.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: hsl(0 0% 0% / 0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal { background: hsl(var(--card)); padding: 24px; border-radius: 8px; max-width: 480px; width: 90%; border: 1px solid hsl(var(--border)); }
.modal h3 { margin-top: 0; margin-bottom: 16px; font-size: 18px; font-weight: 600; }
.modal p { margin-bottom: 12px; font-size: 14px; }
.modal button { width: 100%; }
.test-output { margin-bottom: 12px; }
.test-output pre { background: hsl(var(--muted)); padding: 8px; border-radius: 4px; font-size: 12px; max-height: 120px; overflow-y: auto; white-space: pre-wrap; }
</style>
