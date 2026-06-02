<template>
  <section>
    <header class="row-head">
      <h1>账单中心</h1>
      <button @click="loadAll" :disabled="loading">{{ loading ? "加载中..." : "刷新" }}</button>
    </header>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

    <div class="filters">
      <label>
        日账单日期
        <input v-model="date" type="date" />
      </label>
      <label>
        月账单月份
        <input v-model="month" type="month" />
      </label>
    </div>

    <div class="grid">
      <article class="card" v-if="daily">
        <h2>日账单</h2>
        <p>日期：{{ daily.stat_date }}</p>
        <p>Token 流入：{{ daily.token_in }}</p>
        <p>Token 流出：{{ daily.token_out }}</p>
        <p>调用次数：{{ daily.call_count }}</p>
        <p>充值金额：{{ daily.amount_recharge }}</p>
      </article>

      <article class="card" v-if="monthly">
        <h2>月账单</h2>
        <p>月份：{{ monthly.month }}</p>
        <p>Token 流入：{{ monthly.token_in }}</p>
        <p>Token 流出：{{ monthly.token_out }}</p>
        <p>调用次数：{{ monthly.call_count }}</p>
        <p>充值金额：{{ monthly.amount_recharge }}</p>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from "vue";
import { merchantRequest } from "../utils/http";

interface StatementDaily {
  stat_date: string;
  token_in: number;
  token_out: number;
  call_count: number;
  amount_recharge: string;
}

interface StatementMonthly {
  month: string;
  token_in: number;
  token_out: number;
  call_count: number;
  amount_recharge: string;
}

const today = new Date();
const date = ref(today.toISOString().slice(0, 10));
const month = ref(`${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}`);
const loading = ref(false);
const errorMessage = ref("");
const daily = ref<StatementDaily | null>(null);
const monthly = ref<StatementMonthly | null>(null);

async function loadAll() {
  loading.value = true;
  errorMessage.value = "";
  try {
    const [dailyData, monthlyData] = await Promise.all([
      merchantRequest<StatementDaily>(`/api/v1/billing/statements/daily?date=${date.value}`),
      merchantRequest<StatementMonthly>(`/api/v1/billing/statements/monthly?month=${month.value}`),
    ]);
    daily.value = dailyData;
    monthly.value = monthlyData;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "加载失败";
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  void loadAll();
});
</script>

<style scoped>
.row-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filters {
  margin-top: 12px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

label {
  display: grid;
  gap: 6px;
}

.grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 10px;
}

.card {
  background: #fff;
  border: 1px solid #d8e0f0;
  border-radius: 8px;
  padding: 12px;
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