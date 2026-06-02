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
        <p class="value">{{ enabledPlugins }}</p>
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
      <div class="links">
        <RouterLink to="/plugins/ai-image">AI 图片生成</RouterLink>
        <RouterLink to="/plugins/ai-script">AI 脚本生成</RouterLink>
        <RouterLink to="/billing">账单中心</RouterLink>
      </div>
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
import { merchantRequest } from "../utils/http";

interface BalanceData {
  token_balance: number;
}

interface PluginItem {
  plugin_id: string;
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

const loading = ref(false);
const errorMessage = ref("");
const balance = ref<BalanceData | null>(null);
const plugins = ref<PluginItem[]>([]);
const ledgerItems = ref<LedgerItem[]>([]);

const enabledPlugins = computed(() => plugins.value.filter((item) => item.enabled).length);
const recentDebits = computed(() => ledgerItems.value.filter((item) => item.entry_type === "debit").length);

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
  background: #fff;
  border: 1px solid #d8e0f0;
  border-radius: 8px;
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
  border-bottom: 1px dashed #d8e0f0;
}

button {
  border: none;
  border-radius: 6px;
  padding: 6px 10px;
  background: #2e5fd7;
  color: #fff;
}

.error {
  color: #c83a28;
}

.empty {
  color: #5c6a82;
}
</style>