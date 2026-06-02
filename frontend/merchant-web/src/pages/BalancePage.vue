<template>
  <section>
    <header class="row-head">
      <h1>账户余额</h1>
      <button @click="loadBalance" :disabled="loading">{{ loading ? "加载中..." : "刷新" }}</button>
    </header>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

    <article class="card" v-if="balance">
      <p><strong>Token 余额：</strong>{{ balance.token_balance }}</p>
      <p><strong>存储已用：</strong>{{ balance.storage_used_gb }} GB</p>
      <p><strong>免费额度：</strong>{{ balance.storage_free_quota_gb }} GB</p>
      <p><strong>到期日期：</strong>{{ balance.expire_date }}</p>
      <div class="actions">
        <RouterLink to="/account/recharge">去充值</RouterLink>
        <RouterLink to="/account/ledger">查看流水</RouterLink>
      </div>
    </article>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { merchantRequest } from "../utils/http";

interface BalanceData {
  token_balance: number;
  storage_used_gb: number;
  storage_free_quota_gb: number;
  expire_date: string;
}

const loading = ref(false);
const errorMessage = ref("");
const balance = ref<BalanceData | null>(null);

async function loadBalance() {
  loading.value = true;
  errorMessage.value = "";
  try {
    balance.value = await merchantRequest<BalanceData>("/api/v1/account/balance");
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "加载失败";
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  void loadBalance();
});
</script>

<style scoped>
.row-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card {
  margin-top: 12px;
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: calc(var(--radius) - 2px);
  padding: 14px;
  line-height: 1.8;
}

.actions {
  margin-top: 10px;
  display: flex;
  gap: 12px;
}

.actions a {
  color: hsl(var(--primary));
  text-decoration: none;
  padding: 6px 10px;
  border: 1px solid hsl(var(--border));
  border-radius: calc(var(--radius) - 4px);
  transition: all 0.2s ease;
}

.actions a:hover {
  background: hsl(var(--accent));
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
</style>