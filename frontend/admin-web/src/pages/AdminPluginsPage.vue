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
}

.panel {
  margin-top: 12px;
  background: #fff;
  border: 1px solid #d8e0f0;
  border-radius: 8px;
  padding: 12px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 8px;
}

.table {
  margin-top: 12px;
  width: 100%;
  border-collapse: collapse;
  background: #fff;
}

th,
td {
  border: 1px solid #e5ebf8;
  padding: 8px;
  text-align: left;
  font-size: 13px;
}

.actions {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

button {
  border: none;
  border-radius: 6px;
  padding: 6px 10px;
  background: #2e5fd7;
  color: #fff;
}

button:disabled {
  opacity: 0.6;
}

.error {
  color: #c83a28;
}

.empty {
  margin-top: 12px;
  color: #5c6a82;
}

.status-online {
  color: #52c41a;
}

.status-offline {
  color: #ff4d4f;
}
</style>