<template>
  <section class="sandbox-page">
    <header class="row-head">
      <h1>沙箱测试 — {{ pluginId }}</h1>
      <div class="header-actions">
        <div class="balance-badge">
          <span>测试租户余额：{{ balance }} tokens</span>
          <button @click="loadStatus" :disabled="loadingStatus">刷新</button>
        </div>
        <button @click="clearLogs" class="btn-secondary">清空日志</button>
      </div>
    </header>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

    <div class="sandbox-layout">
      <div class="sandbox-frame-wrap">
        <iframe
          v-if="sandboxUrl"
          :src="sandboxUrl"
          class="sandbox-frame"
          @load="onIframeLoad"
          sandbox="allow-scripts allow-same-origin allow-forms"
        ></iframe>
        <div v-else class="empty-sandbox">
          <p>点击「沙箱测试」按钮加载插件</p>
          <button @click="initSandbox" class="btn-primary" :disabled="loadingSandbox">
            {{ loadingSandbox ? "启动中..." : "启动沙箱" }}
          </button>
        </div>
      </div>

      <div class="log-panel">
        <h3>调用日志</h3>
        <div class="log-list" ref="logListEl">
          <div v-if="logs.length === 0" class="empty-logs">暂无日志</div>
          <div v-for="(log, idx) in logs" :key="idx" class="log-item" :class="'log-' + log.level">
            <div class="log-time">{{ log.time }}</div>
            <div class="log-msg">{{ log.msg }}</div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from "vue";
import { useRoute } from "vue-router";
import { sandboxTest, getPluginStatus } from "../api/plugin";

const route = useRoute();
const pluginId = route.params.pluginId as string;

const sandboxUrl = ref("");
const balance = ref(0);
const loadingSandbox = ref(false);
const loadingStatus = ref(false);
const errorMessage = ref("");
const logs = ref<{ time: string; msg: string; level: string }[]>([]);
const logListEl = ref<HTMLElement | null>(null);

let messageHandler: (e: MessageEvent) => void;

async function initSandbox() {
  loadingSandbox.value = true;
  errorMessage.value = "";
  try {
    const data = await sandboxTest(pluginId);
    sandboxUrl.value = data.sandbox_url;
    balance.value = 10000; // sandbox 默认充值 10000
    addLog("info", `沙箱已启动，测试租户: ${data.test_tenant_name}`);
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : "启动沙箱失败";
    addLog("error", `启动失败: ${errorMessage.value}`);
  } finally {
    loadingSandbox.value = false;
  }
}

async function loadStatus() {
  loadingStatus.value = true;
  try {
    const data = await getPluginStatus(pluginId);
    addLog("info", `状态: ${data.lifecycle_status}，灰度租户: ${data.gray_tenant_count}`);
  } catch {
    addLog("warn", "无法获取插件状态");
  } finally {
    loadingStatus.value = false;
  }
}

function onIframeLoad() {
  addLog("info", "插件 iframe 加载完成");
}

function clearLogs() {
  logs.value = [];
}

function addLog(level: string, msg: string) {
  const now = new Date();
  const time = `${now.getHours().toString().padStart(2, "0")}:${now.getMinutes().toString().padStart(2, "0")}:${now.getSeconds().toString().padStart(2, "0")}`;
  logs.value.push({ time, msg, level });
  if (logListEl.value) {
    setTimeout(() => {
      logListEl.value!.scrollTop = logListEl.value!.scrollHeight;
    }, 0);
  }
}

onMounted(() => {
  messageHandler = (e: MessageEvent) => {
    const { type, payload } = e.data || {};
    if (type === "PLUGIN_READY") {
      addLog("info", "插件发送 PLUGIN_READY（加载完成）");
    } else if (type === "INVOKE_RESULT") {
      addLog("info", `调用结果: success=${payload?.success}, tokenUsed=${payload?.tokenUsed}, balance=${payload?.balanceRemaining}`);
    } else if (type === "ERROR") {
      addLog("error", `插件错误: code=${payload?.code}, msg=${payload?.message}`);
    } else if (type === "PLUGIN_STATUS") {
      addLog("info", `插件状态: ${payload?.status}`);
    }
  };
  window.addEventListener("message", messageHandler);
  void initSandbox();
});

onUnmounted(() => {
  window.removeEventListener("message", messageHandler);
});
</script>

<style scoped>
.sandbox-page {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.row-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}

.row-head h1 {
  font-size: 20px;
  font-weight: 600;
  color: hsl(var(--foreground));
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.balance-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  padding: 6px 12px;
  border-radius: var(--radius);
  font-size: 13px;
  color: hsl(var(--foreground));
}

.balance-badge button {
  border: none;
  border-radius: calc(var(--radius) - 6px);
  padding: 4px 8px;
  background: hsl(var(--secondary));
  color: hsl(var(--foreground));
  font-size: 12px;
  cursor: pointer;
}

.error {
  color: hsl(var(--destructive));
  background: hsl(var(--destructive) / 0.1);
  padding: 12px;
  border-radius: calc(var(--radius) - 4px);
  font-size: 14px;
  margin-bottom: 16px;
}

.sandbox-layout {
  display: flex;
  gap: 16px;
  flex: 1;
  min-height: 0;
}

.sandbox-frame-wrap {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.sandbox-frame {
  flex: 1;
  border: 1px solid hsl(var(--border));
  border-radius: var(--radius);
  min-height: 500px;
  background: hsl(var(--background));
}

.empty-sandbox {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: var(--radius);
  color: hsl(var(--muted-foreground));
}

.log-panel {
  width: 320px;
  flex-shrink: 0;
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: var(--radius);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.log-panel h3 {
  margin: 0;
  padding: 12px 16px;
  font-size: 14px;
  font-weight: 600;
  color: hsl(var(--foreground));
  border-bottom: 1px solid hsl(var(--border));
}

.log-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.empty-logs {
  padding: 24px;
  text-align: center;
  color: hsl(var(--muted-foreground));
  font-size: 13px;
}

.log-item {
  padding: 6px 8px;
  border-radius: calc(var(--radius) - 6px);
  margin-bottom: 4px;
  font-size: 12px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  word-break: break-all;
}

.log-info {
  background: hsl(var(--accent) / 0.15);
  color: hsl(var(--foreground));
}

.log-error {
  background: hsl(var(--destructive) / 0.15);
  color: hsl(var(--destructive));
}

.log-warn {
  background: hsl(38 92% 50% / 0.15);
  color: hsl(38 92% 50%);
}

.log-time {
  font-size: 11px;
  color: hsl(var(--muted-foreground));
}

.log-msg {
  font-size: 12px;
  color: hsl(var(--foreground));
}

.btn-secondary {
  border: none;
  border-radius: calc(var(--radius) - 4px);
  padding: 6px 12px;
  background: hsl(var(--secondary));
  color: hsl(var(--foreground));
  font-size: 13px;
  cursor: pointer;
}

.btn-secondary:hover {
  opacity: 0.9;
}

.btn-primary {
  border: none;
  border-radius: calc(var(--radius) - 4px);
  padding: 8px 16px;
  background: hsl(var(--primary));
  color: hsl(var(--primary-foreground));
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}

.btn-primary:hover:not(:disabled) {
  opacity: 0.9;
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>