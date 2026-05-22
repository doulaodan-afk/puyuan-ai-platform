<template>
  <section>
    <header class="row-head">
      <h1>插件管理</h1>
      <button @click="loadPlugins" :disabled="loading">{{ loading ? "加载中..." : "刷新" }}</button>
    </header>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

    <section class="panel">
      <h2>新增插件</h2>
      <form class="form-grid" @submit.prevent="createPlugin">
        <input v-model.trim="form.plugin_id" placeholder="plugin_id" required />
        <input v-model.trim="form.name" placeholder="插件名称" required />
        <input v-model.trim="form.version" placeholder="版本，如 1.0.0" required />
        <input v-model.trim="form.backend_api" placeholder="backend_api" required />
        <select v-model="form.billing_type">
          <option value="token">token</option>
          <option value="fixed">fixed</option>
        </select>
        <button type="submit" :disabled="submitting">{{ submitting ? "提交中..." : "创建" }}</button>
      </form>
    </section>

    <table v-if="items.length > 0" class="table">
      <thead>
        <tr>
          <th>插件ID</th>
          <th>名称</th>
          <th>版本</th>
          <th>计费</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in items" :key="item.plugin_id">
          <td>{{ item.plugin_id }}</td>
          <td>{{ item.name }}</td>
          <td>{{ item.version }}</td>
          <td>{{ item.billing_type }}</td>
          <td>
            <span :class="item.enabled ? 'status-online' : 'status-offline'">
              {{ item.enabled ? "✅ 上架" : "❌ 下架" }}
            </span>
          </td>
          <td class="actions">
            <button @click="publishPlugin(item.plugin_id)">发布全量</button>
            <button @click="editPlugin(item)">编辑</button>
            <button v-if="item.enabled" @click="removePlugin(item.plugin_id)">下架</button>
            <button v-else @click="restorePlugin(item.plugin_id)">上架</button>
          </td>
        </tr>
      </tbody>
    </table>
    <p v-else class="empty">暂无插件</p>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { adminRequest } from "../utils/http";

interface PluginItem {
  plugin_id: string;
  name: string;
  version: string;
  billing_type: string;
  enabled: boolean;
}

interface PluginListData {
  list: PluginItem[];
}

const loading = ref(false);
const submitting = ref(false);
const errorMessage = ref("");
const items = ref<PluginItem[]>([]);

const form = reactive({
  plugin_id: "",
  name: "",
  version: "1.0.0",
  backend_api: "http://plugin-service/api/run",
  billing_type: "token",
});

async function loadPlugins() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const data = await adminRequest<PluginListData>("/api/v1/admin/plugins?page=1&page_size=50");
    console.log("Plugins loaded:", data.list);
    items.value = data.list;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "加载失败";
  } finally {
    loading.value = false;
  }
}

async function createPlugin() {
  submitting.value = true;
  errorMessage.value = "";
  try {
    await adminRequest("/api/v1/admin/plugins", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(form),
    });

    form.plugin_id = "";
    form.name = "";
    form.version = "1.0.0";
    form.backend_api = "http://plugin-service/api/run";
    form.billing_type = "token";
    await loadPlugins();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "创建失败";
  } finally {
    submitting.value = false;
  }
}

async function publishPlugin(pluginId: string) {
  try {
    await adminRequest(`/api/v1/admin/plugins/${pluginId}/publish`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ mode: "all" }),
    });
    await loadPlugins();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "发布失败";
  }
}

async function editPlugin(item: PluginItem) {
  const name = window.prompt("插件名称", item.name);
  if (!name) {
    return;
  }
  const version = window.prompt("版本", item.version);
  if (!version) {
    return;
  }

  try {
    await adminRequest(`/api/v1/admin/plugins/${item.plugin_id}`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name, version, billing_type: item.billing_type }),
    });
    await loadPlugins();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "更新失败";
  }
}

async function removePlugin(pluginId: string) {
  console.log("Removing plugin:", pluginId);
  try {
    await adminRequest(`/api/v1/admin/plugins/${pluginId}`, {
      method: "DELETE",
    });
    console.log("Plugin removed successfully");
    await loadPlugins();
  } catch (error) {
    console.error("Remove plugin error:", error);
    errorMessage.value = error instanceof Error ? error.message : "下架失败";
  }
}

async function restorePlugin(pluginId: string) {
  console.log("Restoring plugin:", pluginId);
  try {
    await adminRequest(`/api/v1/admin/plugins/${pluginId}`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ status: 1 }),
    });
    console.log("Plugin restored successfully");
    await loadPlugins();
  } catch (error) {
    console.error("Restore plugin error:", error);
    errorMessage.value = error instanceof Error ? error.message : "上架失败";
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

h1,
h2 {
  color: hsl(var(--foreground));
}

h1 {
  font-size: 24px;
  font-weight: 600;
}

h2 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
}

.panel {
  margin-top: 16px;
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: var(--radius);
  padding: 16px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}

input,
select {
  padding: 8px 12px;
  border: 1px solid hsl(var(--input));
  border-radius: calc(var(--radius) - 4px);
  background: hsl(var(--background));
  color: hsl(var(--foreground));
  font-size: 13px;
  transition: all 0.2s ease;
}

input:hover,
select:hover {
  border-color: hsl(var(--ring));
}

input:focus,
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
  border-radius: var(--radius);
  overflow: hidden;
  border: 1px solid hsl(var(--border));
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

.actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

button {
  border: none;
  border-radius: calc(var(--radius) - 4px);
  padding: 6px 10px;
  background: hsl(var(--primary));
  color: hsl(var(--primary-foreground));
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

button:hover:not(:disabled) {
  background: hsl(240 8% 18%);
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error {
  color: hsl(var(--destructive));
  background: hsl(var(--destructive) / 0.1);
  padding: 12px;
  border-radius: calc(var(--radius) - 4px);
  font-size: 14px;
  margin-bottom: 16px;
}

.empty {
  margin-top: 16px;
  color: hsl(var(--muted-foreground));
  text-align: center;
  padding: 48px 24px;
  font-size: 14px;
}

.status-online {
  color: hsl(142 71% 45%);
}

.status-offline {
  color: hsl(var(--destructive));
}
</style>