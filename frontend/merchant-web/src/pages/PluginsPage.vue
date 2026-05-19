<template>
  <section>
    <header class="row-head">
      <h1>插件列表</h1>
      <button @click="loadPlugins" :disabled="loading">{{ loading ? "加载中..." : "刷新" }}</button>
    </header>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

    <ul class="plugin-grid">
      <li v-for="item in plugins" :key="item.plugin_id" class="plugin-card">
        <div>
          <strong>{{ item.name }}</strong>
          <p class="sub">{{ item.plugin_id }} | {{ item.version }} | {{ item.billing_type }}</p>
        </div>
        <div class="actions">
          <span :class="item.enabled ? 'tag-on' : 'tag-off'">{{ item.enabled ? "已启用" : "已禁用" }}</span>
          <button v-if="!item.enabled" @click="togglePlugin(item.plugin_id, true)">启用</button>
          <button v-else @click="togglePlugin(item.plugin_id, false)">禁用</button>
        </div>
      </li>
    </ul>

    <p v-if="!loading && plugins.length === 0" class="empty">暂无插件记录</p>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useAuthStore } from "../stores/auth";

interface PluginItem {
  plugin_id: string;
  name: string;
  version: string;
  billing_type: string;
  enabled: boolean;
}

interface PluginListResponse {
  code: number;
  message: string;
  data: PluginItem[];
}

const auth = useAuthStore();
const plugins = ref<PluginItem[]>([]);
const loading = ref(false);
const errorMessage = ref("");

function buildHeaders(extra?: Record<string, string>): HeadersInit {
  return {
    "X-Tenant-Id": auth.tenantId,
    "X-Request-Id": crypto.randomUUID(),
    Authorization: `Bearer ${auth.accessToken}`,
    ...(extra ?? {}),
  };
}

async function loadPlugins() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const response = await fetch("/api/v1/plugins", {
      method: "GET",
      headers: buildHeaders(),
    });
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }

    const payload = (await response.json()) as PluginListResponse;
    if (payload.code !== 0) {
      throw new Error(payload.message || "load plugin failed");
    }
    plugins.value = payload.data;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "load plugin failed";
  } finally {
    loading.value = false;
  }
}

async function togglePlugin(pluginId: string, enable: boolean) {
  errorMessage.value = "";
  try {
    const response = await fetch(`/api/v1/plugins/${pluginId}/${enable ? "enable" : "disable"}`, {
      method: "POST",
      headers: buildHeaders(),
    });
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }
    await loadPlugins();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "toggle plugin failed";
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
  margin-bottom: 12px;
}

.plugin-grid {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 10px;
}

.plugin-card {
  background: #fff;
  border: 1px solid #d8e0f0;
  border-radius: 8px;
  padding: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.sub {
  color: #5c6a82;
  margin: 4px 0 0;
  font-size: 12px;
}

.actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tag-on,
.tag-off {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 20px;
  font-size: 12px;
}

.tag-on {
  background: #e6f7ef;
  color: #1a7f48;
}

.tag-off {
  background: #ffecec;
  color: #a83838;
}

button {
  border: none;
  border-radius: 6px;
  padding: 6px 10px;
  background: #2e5fd7;
  color: #fff;
  cursor: pointer;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error {
  color: #c83a28;
}

.empty {
  color: #5c6a82;
}
</style>
