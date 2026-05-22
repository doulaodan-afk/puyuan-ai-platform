<template>
  <section>
    <header class="row-head">
      <h1>账单管理</h1>
      <button @click="loadAll" :disabled="loading">{{ loading ? "加载中..." : "刷新" }}</button>
    </header>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

    <section class="cards" v-if="dashboard">
      <article class="card">
        <h3>总调用</h3>
        <p>{{ dashboard.total_calls }}</p>
      </article>
      <article class="card">
        <h3>总 Token</h3>
        <p>{{ dashboard.total_token_used }}</p>
      </article>
      <article class="card">
        <h3>总充值金额</h3>
        <p>{{ dashboard.total_recharge_amount }}</p>
      </article>
      <article class="card">
        <h3>毛利率</h3>
        <p>{{ (dashboard.gross_margin_rate * 100).toFixed(2) }}%</p>
      </article>
    </section>

    <section class="panel">
      <div class="row-head">
        <h2>充值订单</h2>
        <div class="filters">
          <input v-model="tenantId" placeholder="tenant_id（可选）" />
          <button @click="loadOrders">查询订单</button>
        </div>
      </div>

      <table v-if="orders.length > 0" class="table">
        <thead>
          <tr>
            <th>order_no</th>
            <th>tenant_id</th>
            <th>amount</th>
            <th>token_grant</th>
            <th>pay_status</th>
            <th>created_at</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in orders" :key="item.order_no">
            <td>{{ item.order_no }}</td>
            <td>{{ item.tenant_id }}</td>
            <td>{{ item.amount }}</td>
            <td>{{ item.token_grant }}</td>
            <td>{{ item.pay_status }}</td>
            <td>{{ item.created_at }}</td>
          </tr>
        </tbody>
      </table>
      <p v-else class="empty">暂无订单</p>
    </section>
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

interface RechargeOrderItem {
  order_no: string;
  tenant_id: number;
  amount: string;
  token_grant: number;
  pay_status: string;
  created_at: string;
}

interface RechargeOrderPage {
  list: RechargeOrderItem[];
}

const loading = ref(false);
const errorMessage = ref("");
const tenantId = ref("");
const dashboard = ref<DashboardData | null>(null);
const orders = ref<RechargeOrderItem[]>([]);

async function loadAll() {
  loading.value = true;
  errorMessage.value = "";
  try {
    dashboard.value = await adminRequest<DashboardData>("/api/v1/admin/billing/dashboard");
    await loadOrders();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "加载失败";
  } finally {
    loading.value = false;
  }
}

async function loadOrders() {
  try {
    const query = new URLSearchParams({ page: "1", page_size: "20" });
    if (tenantId.value) {
      query.set("tenant_id", tenantId.value);
    }
    const data = await adminRequest<RechargeOrderPage>(`/api/v1/admin/billing/recharge-orders?${query.toString()}`);
    orders.value = data.list;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "加载订单失败";
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

.cards {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 10px;
}

.card,
.panel {
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: calc(var(--radius) - 2px);
  padding: 12px;
}

.panel {
  margin-top: 12px;
}

.card h3,
.panel h2 {
  margin-top: 0;
  margin-bottom: 8px;
  color: hsl(var(--foreground));
  font-size: 14px;
  font-weight: 600;
}

.card p {
  color: hsl(var(--foreground));
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.filters {
  display: flex;
  gap: 8px;
}

.filters input {
  padding: 8px 12px;
  border: 1px solid hsl(var(--border));
  border-radius: calc(var(--radius) - 4px);
  background: hsl(var(--muted));
  color: hsl(var(--foreground));
  font-size: 14px;
}

.table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 10px;
  background: hsl(var(--card));
  border-radius: var(--radius);
  overflow: hidden;
  border: 1px solid hsl(var(--border));
}

thead {
  background: hsl(var(--secondary));
}

th {
  padding: 12px 16px;
  text-align: left;
  font-size: 13px;
  font-weight: 600;
  color: hsl(var(--foreground));
  border-bottom: 2px solid hsl(var(--border));
}

td {
  padding: 12px 16px;
  text-align: left;
  font-size: 13px;
  color: hsl(var(--foreground));
  border-bottom: 1px solid hsl(var(--border) / 0.5);
}

tbody tr:hover {
  background: hsl(var(--accent));
}

tbody tr:last-child td {
  border-bottom: none;
}

button {
  border: none;
  border-radius: calc(var(--radius) - 4px);
  padding: 6px 10px;
  background: hsl(var(--primary));
  color: hsl(var(--primary-foreground));
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

button:hover:not(:disabled) {
  background: hsl(240 8% 18%);
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error {
  color: hsl(var(--destructive));
  background: hsl(var(--destructive) / 0.1);
  padding: 12px;
  border-radius: calc(var(--radius) - 4px);
  font-size: 14px;
  margin-bottom: 16px;
  border: 1px solid hsl(var(--destructive) / 0.3);
}

.empty {
  color: hsl(var(--muted-foreground));
  text-align: center;
  padding: 48px 24px;
  font-size: 14px;
}
</style>