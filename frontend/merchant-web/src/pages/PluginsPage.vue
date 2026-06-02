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
            <span v-if="item.enabled" class="tenant-enabled-badge">已启用</span>
            <span v-else class="tenant-disabled-badge">未启用</span>
          </div>
          <p class="sub">{{ item.plugin_id }} | v{{ item.version }}</p>
          <p class="billing-hint">{{ billingHint(item.billing_type) }}</p>
        </div>
        <div class="actions" @click.stop>
          <button v-if="item.enabled" class="btn-enter" @click="openPlugin(item)">{{ item.lifecycle_status === 'gray' ? '灰度体验' : '立即使用' }}</button>
          <button v-if="item.enabled" class="btn-disable" @click="disablePlugin(item)">停用</button>
          <button v-if="!item.enabled" class="btn-enable" @click="enablePlugin(item)">启用</button>
        </div>
      </li>
    </ul>

    <p v-if="!loading && plugins.length === 0" class="empty">暂无可用插件</p>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { merchantRequest } from "../utils/http";

interface PluginItem {
  plugin_id: string;
  name: string;
  version: string;
  billing_type: string;
  lifecycle_status: "enabled" | "gray";
  enabled: boolean;
}

const router = useRouter();
const plugins = ref<PluginItem[]>([]);
const loading = ref(false);
const errorMessage = ref("");

// 插件ID到路由的映射（key 必须与后端 API 返回的 plugin_id 格式一致，使用连字符）
const PLUGIN_ROUTES: Record<string, string> = {
  "ai-script-gen": "/plugins/ai-script",
  "ai-translate": "/plugins/ai-translate",
  "ai-design-assistant": "/plugins/ai-design-assistant",
  "ai-image-gen": "/plugins/ai-image-gen",
  "acme.ai-image-gen": "/plugins/ai-image-gen",
};

function openPlugin(item: PluginItem) {
  const routePath = PLUGIN_ROUTES[item.plugin_id] || `/plugins/${item.plugin_id}`;
  router.push(routePath);
}

async function enablePlugin(item: PluginItem) {
  try {
    await merchantRequest<{ plugin_id: string; enabled: boolean }>(`/api/v1/plugins/${item.plugin_id}/enable`, {
      method: "POST",
    });
    item.enabled = true;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "启用失败";
  }
}

async function disablePlugin(item: PluginItem) {
  try {
    await merchantRequest<{ plugin_id: string; enabled: boolean }>(`/api/v1/plugins/${item.plugin_id}/disable`, {
      method: "POST",
    });
    item.enabled = false;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "停用失败";
  }
}

async function loadPlugins() {
  loading.value = true;
  errorMessage.value = "";
  try {
    plugins.value = await merchantRequest<PluginItem[]>("/api/v1/plugins/visible");
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "加载失败";
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
  display: flex;
  gap: 8px;
}

.btn-enter {
  border: none;
  border-radius: calc(var(--radius) - 4px);
  padding: 8px 16px;
  background: hsl(var(--primary));
  color: hsl(var(--primary-foreground));
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
}

.btn-enter:hover {
  opacity: 0.9;
}

.btn-enable {
  border: none;
  border-radius: calc(var(--radius) - 4px);
  padding: 8px 16px;
  background: hsl(142 71% 45%);
  color: white;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
}

.btn-enable:hover {
  opacity: 0.9;
}

.btn-disable {
  border: 1px solid hsl(var(--border));
  border-radius: calc(var(--radius) - 4px);
  padding: 8px 16px;
  background: transparent;
  color: hsl(var(--muted-foreground));
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
}

.btn-disable:hover {
  border-color: hsl(var(--destructive));
  color: hsl(var(--destructive));
}

.tenant-enabled-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  background: hsl(142 71% 45% / 0.15);
  color: hsl(142 71% 45%);
}

.tenant-disabled-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  background: hsl(var(--muted-foreground) / 0.15);
  color: hsl(var(--muted-foreground));
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