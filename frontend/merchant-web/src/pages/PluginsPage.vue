<template>
  <section>
    <header class="row-head">
      <h1>应用市场</h1>
      <button @click="loadPlugins" :disabled="loading">{{ loading ? "加载中..." : "刷新" }}</button>
    </header>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

    <ul class="plugin-grid">
      <li v-for="item in plugins" :key="item.plugin_id" class="plugin-card">
        <div class="plugin-info" @click="openPlugin(item)">
          <div class="plugin-header">
            <strong class="plugin-name">{{ item.name }}</strong>
            <span :class="'status-' + item.lifecycle_status">{{ statusText(item.lifecycle_status) }}</span>
          </div>
          <p class="sub">{{ item.plugin_id }} | v{{ item.version }}</p>
          <p class="billing-hint">{{ billingHint(item.billing_type) }}</p>
        </div>
        <div class="actions" @click.stop>
          <button @click="openPlugin(item)">{{ item.lifecycle_status === 'gray' ? '灰度体验' : '立即使用' }}</button>
        </div>
      </li>
    </ul>

    <p v-if="!loading && plugins.length === 0" class="empty">暂无可用插件</p>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "../stores/auth";

interface PluginItem {
  plugin_id: string;
  name: string;
  version: string;
  billing_type: string;
  lifecycle_status: "enabled" | "gray";
}

interface PluginListResponse {
  code: number;
  message: string;
  data: PluginItem[];
}

const router = useRouter();
const auth = useAuthStore();
const plugins = ref<PluginItem[]>([]);
const loading = ref(false);
const errorMessage = ref("");

// 插件ID到路由的映射
const PLUGIN_ROUTES: Record<string, string> = {
  "ai_script_gen": "/plugins/ai-script",
  "ai_translate": "/plugins/ai-translate",
  "ai_design_assistant": "/design-assistant",
  "ai_image_gen": "/plugins/ai-image",
};

function openPlugin(item: PluginItem) {
  const routePath = PLUGIN_ROUTES[item.plugin_id] || `/plugins/${item.plugin_id}`;
  router.push(routePath);
}

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
    const response = await fetch("/api/v1/plugins/visible", {
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

function statusText(status: string): string {
  const map: Record<string, string> = {
    enabled: "已上架",
    gray: "灰度测试中",
  };
  return map[status] || status;
}

function billingHint(billingType: string): string {
  const map: Record<string, string> = {
    "token": "按 Token 计费",
    "subscription": "订阅制",
    "one_time": "一次性付费",
  };
  return map[billingType] || billingType;
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

h1 {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
}

.plugin-grid {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 12px;
}

.plugin-card {
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: calc(var(--radius) - 2px);
  padding: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  transition: box-shadow 0.2s ease, border-color 0.2s ease;
}

.plugin-card:hover {
  border-color: hsl(var(--accent-blue));
  box-shadow: var(--shadow-md);
}

.plugin-info {
  flex: 1;
  min-width: 0;
}

.plugin-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
}

.plugin-name {
  font-size: 15px;
  font-weight: 600;
}

.status-enabled {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  background: hsl(142 71% 45% / 0.15);
  color: hsl(142 71% 45%);
}

.status-gray {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  background: hsl(260 60% 60% / 0.15);
  color: hsl(260 60% 60%);
}

.sub {
  color: hsl(var(--muted-foreground));
  margin: 4px 0 0;
  font-size: 12px;
}

.billing-hint {
  color: hsl(var(--muted-foreground));
  margin: 2px 0 0;
  font-size: 12px;
}

.actions {
  flex-shrink: 0;
}

.actions button {
  border: none;
  border-radius: calc(var(--radius) - 4px);
  padding: 8px 16px;
  background: hsl(var(--primary));
  color: hsl(var(--primary-foreground));
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
}

.actions button:hover {
  opacity: 0.9;
}

.error {
  color: hsl(var(--destructive));
  margin-bottom: 12px;
}

.empty {
  color: hsl(var(--muted-foreground));
  text-align: center;
  padding: 48px 24px;
}
</style>