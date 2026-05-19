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
}

.cards {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 10px;
}

.card {
  background: #fff;
  border: 1px solid #d8e0f0;
  border-radius: 8px;
  padding: 12px;
}

.value {
  font-size: 24px;
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
</style>