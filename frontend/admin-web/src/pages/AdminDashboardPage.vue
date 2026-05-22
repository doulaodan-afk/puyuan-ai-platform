<template>
  <section>
    <header class="row-head">
      <h1>运营看板</h1>
      <button @click="loadData" :disabled="loading">{{ loading ? "加载中..." : "刷新" }}</button>
    </header>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

    <div class="cards" v-if="dashboard">
      <article class="card">
        <h3>总调用数</h3>
        <p class="value">{{ dashboard.total_calls }}</p>
      </article>
      <article class="card">
        <h3>总消耗 Token</h3>
        <p class="value">{{ dashboard.total_token_used }}</p>
      </article>
      <article class="card">
        <h3>总充值金额</h3>
        <p class="value">{{ dashboard.total_recharge_amount }}</p>
      </article>
      <article class="card">
        <h3>毛利率</h3>
        <p class="value">{{ (dashboard.gross_margin_rate * 100).toFixed(2) }}%</p>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { adminRequest } from "../utils/http";

interface DashboardData {
  total_calls: number;
  total_token_used: number;
  total_recharge_amount: string;
  gross_margin_rate: number;
}

const loading = ref(false);
const errorMessage = ref("");
const dashboard = ref<DashboardData | null>(null);

async function loadData() {
  loading.value = true;
  errorMessage.value = "";
  try {
    dashboard.value = await adminRequest<DashboardData>("/api/v1/admin/billing/dashboard");
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "加载失败";
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  void loadData();
});
</script>

<style scoped>
.row-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

h1 {
  color: hsl(var(--foreground));
  font-size: 24px;
  font-weight: 600;
  margin: 0;
}

.cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.card {
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: var(--radius);
  padding: 20px;
  /* 添加内阴影增加层次感 */
  box-shadow: inset 0 1px 2px hsl(0 0% 0% / 0.05);
}

.card h3 {
  margin: 0 0 8px 0;
  color: hsl(var(--muted-foreground));
  font-size: 14px;
  font-weight: 500;
}

.value {
  font-size: 28px;
  font-weight: 600;
  color: hsl(var(--foreground));
}

.btn {
  padding: 8px 16px;
  background: hsl(var(--primary));
  color: hsl(var(--primary-foreground));
  border: none;
  border-radius: calc(var(--radius) - 4px);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn:hover:not(:disabled) {
  opacity: 0.9;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error {
  color: hsl(var(--destructive));
  background: hsl(var(--destructive) / 0.1);
  padding: 12px;
  border-radius: var(--radius);
  font-size: 14px;
  margin-bottom: 16px;
  border: 1px solid hsl(var(--destructive) / 0.3);
}
</style>