<template>
  <section class="dashboard">
    <header class="row-head">
      <h1>工作台</h1>
      <button @click="refreshAll" :disabled="loading">{{ loading ? "加载中..." : "刷新" }}</button>
    </header>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

    <div class="cards">
      <article class="card">
        <h3>Token 余额</h3>
        <p class="value">{{ balance?.token_balance ?? 0 }}</p>
        <RouterLink to="/account/recharge">去充值</RouterLink>
      </article>
      <article class="card">
        <h3>已启用插件</h3>
        <p class="value">{{ enabledPlugins.length }}</p>
        <RouterLink to="/plugins">管理插件</RouterLink>
      </article>
      <article class="card">
        <h3>最近调用</h3>
        <p class="value">{{ recentDebits }}</p>
        <RouterLink to="/account/ledger">查看流水</RouterLink>
      </article>
    </div>

    <section class="panel">
      <h2>快捷入口</h2>
      <div class="links" v-if="enabledPlugins.length > 0">
        <div
          v-for="plugin in enabledPlugins"
          :key="plugin.plugin_id"
          class="plugin-link"
          @click="openPlugin(plugin.plugin_id)"
        >
          <span class="plugin-name">{{ plugin.name }}</span>
          <span class="plugin-badge">已启用</span>
        </div>
      </div>
      <p v-else class="empty">暂无可用插件</p>
    </section>

    <section class="panel">
      <h2>最近流水</h2>
      <ul v-if="ledgerItems.length > 0" class="ledger-list">
        <li v-for="item in ledgerItems" :key="item.biz_no">
          <span>{{ item.entry_type }}</span>
          <span>{{ item.direction === "in" ? "+" : "-" }}{{ item.token_amount }}</span>
          <span>{{ item.occurred_at ?? "-" }}</span>
        </li>
      </ul>
      <p v-else class="empty">暂无流水记录</p>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { merchantRequest } from "../utils/http";

interface BalanceData {
  token_balance: number;
}

interface PluginItem {
  plugin_id: string;
  name: string;
  version: string;
  billing_type: string;
  enabled: boolean;
}

interface LedgerItem {
  biz_no: string;
  entry_type: string;
  direction: string;
  token_amount: number;
  occurred_at: string | null;
}

interface LedgerData {
  list: LedgerItem[];
}

const router = useRouter();
const loading = ref(false);
const errorMessage = ref("");
const balance = ref<BalanceData | null>(null);
const plugins = ref<PluginItem[]>([]);
const ledgerItems = ref<LedgerItem[]>([]);

// 插件ID到路由的映射
const PLUGIN_ROUTES: Record<string, string> = {
  "ai_script_gen": "/plugins/ai-script",
  "ai_translate": "/plugins/ai-translate",
  "ai_design_assistant": "/design-assistant",
  "ai_image_gen": "/plugins/ai-image",
};

const enabledPlugins = computed(() => plugins.value.filter((item) => item.enabled));
const recentDebits = computed(() => ledgerItems.value.filter((item) => item.entry_type === "debit").length);

function openPlugin(pluginId: string) {
  const routePath = PLUGIN_ROUTES[pluginId];
  if (routePath) {
    router.push(routePath);
  } else {
    console.log("打开插件:", pluginId);
  }
}

async function refreshAll() {
  loading.value = true;
  errorMessage.value = "";

  try {
    const [balanceData, pluginData, ledgerData] = await Promise.all([
      merchantRequest<BalanceData>("/api/v1/account/balance"),
      merchantRequest<PluginItem[]>("/api/v1/plugins"),
      merchantRequest<LedgerData>("/api/v1/account/ledger?page=1&page_size=8"),
    ]);

    balance.value = balanceData;
    plugins.value = pluginData;
    ledgerItems.value = ledgerData.list;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "Load failed";
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  void refreshAll();
});
</script>

<style scoped>
.row-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.cards {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.card,
.panel {
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: var(--radius);
  padding: 12px;
}

.value {
  font-size: 24px;
  margin: 8px 0;
}

.panel {
  margin-top: 12px;
}

.links {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.plugin-link {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: hsl(var(--accent));
  border: 1px solid hsl(var(--border));
  border-radius: calc(var(--radius) - 4px);
  cursor: pointer;
  transition: all 0.2s ease;
}

.plugin-link:hover {
  background: hsl(var(--accent) / 0.8);
  border-color: hsl(var(--primary));
}

.plugin-name {
  font-size: 14px;
  font-weight: 500;
  color: hsl(var(--foreground));
}

.plugin-badge {
  font-size: 12px;
  padding: 2px 8px;
  background: hsl(var(--success) / 0.2);
  color: hsl(var(--success));
  border-radius: 9999px;
}

.ledger-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.ledger-list li {
  display: grid;
  grid-template-columns: 120px 100px 1fr;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px dashed hsl(var(--border) / 0.5);
}

button {
  border: none;
  border-radius: calc(var(--radius) - 4px);
  padding: 6px 10px;
  background: hsl(var(--primary));
  color: hsl(var(--primary-foreground));
}

.error {
  color: hsl(var(--destructive));
}

.empty {
  color: hsl(var(--muted-foreground));
}

a {
  color: hsl(var(--primary));
  text-decoration: none;
}

a:hover {
  text-decoration: underline;
}
</style>
