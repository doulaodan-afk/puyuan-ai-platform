<template>
  <section class="page-container">
    <header class="row-head">
      <h1>系统配置管理</h1>
    </header>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

    <!-- 配置类型切换 -->
    <div class="tabs">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        :class="{ active: activeTab === tab.value }"
        @click="switchTab(tab.value)"
      >
        {{ tab.label }}
      </button>
    </div>

    <!-- 对象存储 (OSS) 配置 -->
    <section v-if="activeTab === 'oss'" class="panel">
      <h2>对象存储 (OSS) 配置 — 七牛云</h2>

      <!-- 添加/编辑表单 -->
      <div v-if="showForm" class="form-panel">
        <h3>{{ editingId ? '编辑配置' : '添加配置' }}</h3>
        <form class="form-grid" @submit.prevent="saveOssConfig">
          <input v-model.trim="ossForm.providerName" placeholder="提供商名称 (如 Qiniu)" required />
          <input
            v-model.trim="ossForm.accessKey"
            type="password"
            placeholder="Access Key"
            required
          />
          <input
            v-model.trim="ossForm.secretKey"
            type="password"
            placeholder="Secret Key"
            required
          />
          <input v-model.trim="ossForm.bucket" placeholder="Bucket 名称 (如 puyuanmaoshan)" required />
          <input v-model.trim="ossForm.cdnDomain" placeholder="CDN 域名 (如 www-cdn.puyuanmaoshan.com)" required />
          <input v-model.number="ossForm.priority" type="number" placeholder="优先级 (1-10)" min="1" max="10" />
          <label class="checkbox">
            <input v-model="ossForm.enabled" type="checkbox" />
            启用
          </label>
          <div class="form-actions">
            <button type="submit">{{ editingId ? '更新' : '保存' }}</button>
            <button type="button" @click="showForm = false">取消</button>
          </div>
        </form>
      </div>

      <!-- 配置列表 -->
      <div v-else>
        <button @click="showAddForm" class="add-btn">添加配置</button>
        <table v-if="ossConfigs.length > 0" class="table">
          <thead>
            <tr>
              <th>提供商</th>
              <th>Access Key</th>
              <th>Secret Key</th>
              <th>Bucket</th>
              <th>CDN 域名</th>
              <th>优先级</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(config, index) in ossConfigs" :key="index">
              <td>{{ config.provider_name }}</td>
              <td class="masked">{{ config.access_key }}</td>
              <td class="masked">{{ config.secret_key }}</td>
              <td>{{ config.bucket }}</td>
              <td>{{ config.cdn_domain }}</td>
              <td>{{ config.priority }}</td>
              <td>
                <span :class="config.enabled ? 'status-online' : 'status-offline'">
                  {{ config.enabled ? '启用' : '禁用' }}
                </span>
              </td>
              <td class="actions">
                <button @click="editConfig(config)">编辑</button>
                <button @click="testConfig(config)">测试</button>
                <button @click="deleteConfig(config)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-else class="empty">暂无配置，点击"添加配置"开始</p>
      </div>
    </section>

    <!-- 测试结果对话框 -->
    <div v-if="testResult" class="modal-overlay" @click="closeTestResult">
      <div class="modal" @click.stop>
        <h3>测试结果</h3>
        <p :class="testResult.success ? 'success' : 'error'">
          {{ testResult.success ? '✅' : '❌' }} {{ testResult.message }}
        </p>
        <p v-if="testResult.latency !== null">响应时间: {{ testResult.latency }}ms</p>
        <button @click="closeTestResult">关闭</button>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import {
  type OssConfig,
  type TestConfigResponse,
  deleteConfig as deleteConfigApi,
  getOssConfigs,
  saveOssConfig as saveOssConfigApi,
  testConfig as testConfigApi,
} from "../api/systemConfig";

interface Tabs {
  value: string;
  label: string;
}

const tabs: Tabs[] = [
  { value: "oss", label: "对象存储" },
];

const activeTab = ref("oss");
const errorMessage = ref("");
const showForm = ref(false);
const editingId = ref<number | null>(null);
const testResult = ref<TestConfigResponse | null>(null);

const ossConfigs = ref<OssConfig[]>([]);

const ossForm = reactive({
  providerName: "",
  accessKey: "",
  secretKey: "",
  bucket: "",
  cdnDomain: "",
  priority: 1,
  enabled: true,
});

function switchTab(tab: string) {
  activeTab.value = tab;
  showForm.value = false;
  editingId.value = null;
  errorMessage.value = "";
  loadConfigs();
}

async function loadConfigs() {
  errorMessage.value = "";
  try {
    if (activeTab.value === "oss") {
      ossConfigs.value = await getOssConfigs();
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "加载失败";
  }
}

function showAddForm() {
  showForm.value = true;
  editingId.value = null;
  resetForms();
}

function resetForms() {
  ossForm.providerName = "";
  ossForm.accessKey = "";
  ossForm.secretKey = "";
  ossForm.bucket = "";
  ossForm.cdnDomain = "";
  ossForm.priority = 1;
  ossForm.enabled = true;
}

function editConfig(config: OssConfig) {
  showForm.value = true;
  editingId.value = config.config_id ?? null;

  ossForm.providerName = config.provider_name;
  ossForm.accessKey = config.access_key;
  ossForm.secretKey = config.secret_key;
  ossForm.bucket = config.bucket;
  ossForm.cdnDomain = config.cdn_domain;
  ossForm.priority = config.priority;
  ossForm.enabled = config.enabled;
}

async function saveOssConfig() {
  try {
    await saveOssConfigApi({
      provider_name: ossForm.providerName,
      access_key: ossForm.accessKey,
      secret_key: ossForm.secretKey,
      bucket: ossForm.bucket,
      cdn_domain: ossForm.cdnDomain,
      priority: ossForm.priority,
      enabled: ossForm.enabled,
    });
    showForm.value = false;
    resetForms();
    await loadConfigs();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "保存失败";
  }
}

async function deleteConfig(config: OssConfig) {
  if (!confirm("确定要删除此配置吗？")) {
    return;
  }

  if (config.config_id === null) {
    errorMessage.value = "无法删除：配置 ID 为空";
    return;
  }

  try {
    await deleteConfigApi(config.config_id);
    await loadConfigs();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "删除失败";
  }
}

async function testConfig(config: OssConfig) {
  if (config.config_id === null) {
    testResult.value = {
      success: false,
      message: "无法测试：配置 ID 为空",
      latency: null,
    };
    return;
  }

  try {
    testResult.value = await testConfigApi({ id: config.config_id });
  } catch (error) {
    testResult.value = {
      success: false,
      message: error instanceof Error ? error.message : "测试失败",
      latency: null,
    };
  }
}

function closeTestResult() {
  testResult.value = null;
}

onMounted(() => {
  void loadConfigs();
});
</script>

<style scoped>
.row-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.row-head h1 {
  font-size: 20px;
  font-weight: 600;
  color: hsl(var(--foreground));
}

@media (min-width: 768px) {
  .row-head h1 {
    font-size: 28px;
  }
}

@media (min-width: 1280px) {
  .row-head h1 {
    font-size: 32px;
  }
}

.error {
  background: hsl(var(--destructive) / 0.1);
  color: hsl(var(--destructive));
  padding: 12px 16px;
  border-radius: calc(var(--radius) - 4px);
  margin-bottom: 16px;
  border: 1px solid hsl(var(--destructive) / 0.2);
}

.tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.tabs button {
  padding: 8px 16px;
  border: none;
  border-radius: calc(var(--radius) - 4px);
  background: hsl(var(--muted));
  color: hsl(var(--muted-foreground));
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: background 0.2s ease;
}

@media (min-width: 768px) {
  .tabs button {
    padding: 10px 20px;
    font-size: 15px;
  }
}

.tabs button:hover:not(.active) {
  background: hsl(var(--accent));
}

.tabs button.active {
  background: hsl(var(--primary));
  color: hsl(var(--primary-foreground));
}

.panel {
  margin-top: 20px;
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: var(--radius);
  padding: 16px;
}

@media (min-width: 768px) {
  .panel {
    padding: 24px;
    margin-top: 24px;
  }
}

@media (min-width: 1280px) {
  .panel {
    padding: 28px;
    margin-top: 28px;
  }
}

.panel h2 {
  margin-top: 0;
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 600;
  color: hsl(var(--foreground));
}

@media (min-width: 768px) {
  .panel h2 {
    font-size: 18px;
  }
}

@media (min-width: 1280px) {
  .panel h2 {
    font-size: 20px;
  }
}

.form-panel {
  background: hsl(var(--muted));
  padding: 16px;
  border-radius: var(--radius);
  margin-bottom: 16px;
  border: 1px solid hsl(var(--border));
}

@media (min-width: 768px) {
  .form-panel {
    padding: 20px;
    margin-bottom: 20px;
  }
}

.form-panel h3 {
  margin-top: 0;
  margin-bottom: 16px;
  font-size: 15px;
  font-weight: 600;
  color: hsl(var(--foreground));
}

@media (min-width: 768px) {
  .form-panel h3 {
    font-size: 16px;
  }
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
  margin-bottom: 16px;
}

@media (min-width: 640px) {
  .form-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (min-width: 1024px) {
  .form-grid {
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 16px;
  }
}

.form-grid input[type="text"],
.form-grid input[type="password"],
.form-grid input[type="number"] {
  padding: 10px 14px;
  border: 1px solid hsl(var(--border));
  border-radius: calc(var(--radius) - 4px);
  font-size: 14px;
  color: hsl(var(--foreground));
  background: hsl(var(--background));
}

@media (min-width: 768px) {
  .form-grid input[type="text"],
  .form-grid input[type="password"],
  .form-grid input[type="number"] {
    padding: 12px 16px;
    font-size: 15px;
  }
}

.checkbox {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: hsl(var(--foreground));
}

@media (min-width: 768px) {
  .checkbox {
    font-size: 15px;
  }
}

.form-actions {
  display: flex;
  gap: 8px;
}

.add-btn {
  margin-bottom: 16px;
}

@media (min-width: 768px) {
  .add-btn {
    margin-bottom: 20px;
  }
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

th,
td {
  border-bottom: 1px solid hsl(var(--border) / 0.5);
  padding: 12px;
  text-align: left;
  font-size: 14px;
  color: hsl(var(--foreground));
}

@media (min-width: 768px) {
  th,
  td {
    padding: 14px;
    font-size: 15px;
  }
}

th {
  font-weight: 600;
  font-size: 13px;
  color: hsl(var(--foreground));
}

tbody tr:hover {
  background: hsl(var(--accent));
}

.masked {
  font-family: "Consolas", monospace;
  color: hsl(var(--muted-foreground));
}

.actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
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
  transition: background 0.2s ease;
}

button:hover:not(:disabled) {
  background: hsl(240 8% 18%);
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

button[type="button"] {
  background: hsl(var(--secondary));
  color: hsl(var(--secondary-foreground));
}

button[type="button"]:hover:not(:disabled) {
  background: hsl(var(--accent));
}

.success {
  color: hsl(142 71% 45%);
  font-weight: 500;
}

.empty {
  margin-top: 16px;
  color: hsl(var(--muted-foreground));
  font-size: 14px;
  text-align: center;
  padding: 40px 0;
}

@media (min-width: 768px) {
  .empty {
    font-size: 15px;
    margin-top: 20px;
  }
}

.status-online {
  color: hsl(142 71% 45%);
  font-weight: 500;
}

.status-offline {
  color: hsl(var(--destructive));
  font-weight: 500;
}

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
}

.modal {
  background: hsl(var(--card));
  padding: 24px;
  border-radius: var(--radius);
  max-width: 400px;
  width: 90%;
  border: 1px solid hsl(var(--border));
}

.modal h3 {
  margin-top: 0;
  margin-bottom: 16px;
  font-size: 18px;
  font-weight: 600;
  color: hsl(var(--foreground));
}

.modal p {
  margin-bottom: 12px;
  font-size: 14px;
  color: hsl(var(--foreground));
}

.modal button {
  width: 100%;
}
</style>